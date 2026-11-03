# 05 · Mensajería asíncrona con RabbitMQ (EP3)

GestorCitas usa RabbitMQ para las tareas que no deben bloquear la respuesta al usuario:
avisarle al paciente, generar el comprobante de su reserva y auditar los cambios de usuarios.
Los productores publican **después del commit** y nunca fallan por culpa del broker: si RabbitMQ
no está disponible, la reserva igual se guarda y el error queda en el log.

```
 ms-citas ──(topic)  gestorcitas.citas.eventos      cita.*               ──► q.notificaciones.citas ──► NotificacionCitaConsumer
          └─(direct) gestorcitas.citas.comprobantes comprobante.generar  ──► q.comprobantes.citas   ──► ComprobanteCitaConsumer
 ms-usuarios (topic) gestorcitas.usuarios.eventos   usuario.#            ──► q.auditoria.usuarios   ──► AuditoriaUsuarioConsumer
                                                                                   │ error
                         ┌─────────────── recuperable y quedan intentos ───────────┤
                         ▼                                                         │ no recuperable / sin intentos
                 q.<flujo>.retry (TTL 5 s) ──► vuelve a q.<flujo>                   ▼
                                                               basicNack(requeue=false)
                                                                         │
                                                       gestorcitas.dlx (direct) ──► q.<flujo>.dlq
```

## Microservicios

| Servicio | Puerto | Rol en la mensajería |
|---|---|---|
| `ms-citas` | 8082 | **Productor**: eventos de citas (topic) y solicitudes de comprobante (direct) |
| `ms-usuarios` | 8084 | **Productor**: eventos de usuarios para auditoría (topic) |
| `ms-notificaciones` | 8086 | **Consumidor**: declara colas, bindings, colas de reintento y DLQ; procesa con ACK manual |
| `ms-rabbit-admin` | 8085 | **Administrador**: API REST para crear y eliminar colas, exchanges y bindings |

## Dónde está cada cosa (mapa para la rúbrica)

| Indicador | Dónde mirar |
|---|---|
| Nombres centralizados (12%) | `application.yml` → `app.messaging` de cada microservicio, leídos con `MessagingProperties` (`@ConfigurationProperties`). Los `@RabbitListener` usan `${app.messaging.<flujo>.cola}` |
| Beans Queue / Exchange / Binding por caso de uso (13%) | `ms-notificaciones/.../messaging/config/RabbitConfig.java` (3 flujos: exchange, cola, cola de reintento, DLQ, binding y binding de DLQ). Productores: `ms-citas` y `ms-usuarios` → `messaging/config/RabbitConfig.java` |
| Configuración separada del negocio (10%) | Los servicios de negocio solo usan las interfaces `CitaEventPublisher` y `UsuarioEventPublisher`. RabbitMQ vive en el paquete `messaging` |
| Consumidores con `@RabbitListener` agrupados por dominio (15%) | `messaging/consumer/citas/` (notificaciones y comprobantes) y `messaging/consumer/usuarios/` (auditoría) |
| ACK y manejo de errores (20%) | `ms-notificaciones/.../messaging/support/ProcesadorDeMensajes.java` (tabla de decisiones en su Javadoc) |
| Endpoints REST del administrador (13%) | `ms-rabbit-admin/.../controller/` |
| Lógica en un servicio dedicado (10%) | `RabbitAdminService` / `RabbitAdminServiceImpl` |
| Validación de entrada (7%) | DTOs con Bean Validation (`CrearColaRequest`, `CrearExchangeRequest`, `BindingRequest`, `@NombreAmqp`) + reglas del servicio (duplicados, existencia, recursos protegidos, comodines) |

## Política de confirmación (ACK)

`spring.rabbitmq.listener.simple.acknowledge-mode: manual`. Cada mensaje termina en uno de estos caminos:

| Situación | Acción | Resultado |
|---|---|---|
| Procesado sin errores | `basicAck` | Sale de la cola |
| JSON mal formado o campos obligatorios faltantes (`MensajeInvalidoException`) | `basicNack(requeue=false)` | **DLQ inmediata** (reintentar no sirve) |
| Error temporal (`ErrorTransitorioException` u otro) y quedan intentos | Copia a `q.<flujo>.retry` con header `x-gestorcitas-intento+1` y `basicAck` | Vuelve a la cola principal tras el TTL (5 s) |
| Error temporal y se agotaron los intentos (3) | `basicNack(requeue=false)` | **DLQ** |

Nunca se usa `requeue=true`, para evitar bucles infinitos. Cada envío a la DLQ se registra con el
prefijo `[DLQ]`, el motivo y el contenido del mensaje. Del lado del productor, `ConfirmCallback` y
`ReturnsCallback` (`mandatory=true`) registran los mensajes que el broker rechaza o no puede enrutar.

## API de administración

El BFF la publica bajo `/api/admin/mensajeria` (JWT de Entra ID + rol **Admin**), y ms-rabbit-admin la atiende en su raíz.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/queues` | Lista colas con mensajes y consumidores | 200 |
| GET | `/queues/{nombre}` | Detalle de una cola | 200 · 404 |
| POST | `/queues` | Crea una cola (`nombre`, `durable`, `autoDelete`, `ttlMs`, `maxMensajes`, `deadLetterExchange`, `deadLetterRoutingKey`) | 201 · 400 · 404 (DLX) · 409 |
| DELETE | `/queues/{nombre}?soloSiVacia=&soloSiSinConsumidores=` | Elimina una cola | 204 · 404 · 409 |
| DELETE | `/queues/{nombre}/messages` | Vacía la cola | 200 · 404 |
| GET | `/exchanges` | Lista exchanges (sin `""` ni `amq.*`) | 200 |
| POST | `/exchanges` | Crea un exchange (`nombre`, `tipo`: direct/topic/fanout/headers) | 201 · 400 · 409 |
| DELETE | `/exchanges/{nombre}` | Elimina un exchange | 204 · 404 · 409 |
| GET | `/bindings` | Lista bindings exchange → cola | 200 |
| POST | `/bindings` | Crea un binding (`exchange`, `cola`, `routingKey`) | 201 · 400 · 404 · 409 |
| DELETE | `/bindings?exchange=&cola=&routingKey=` | Elimina un binding | 204 · 404 · 409 |
| POST | `/messages` | Publica un mensaje de prueba (`exchange`, `routingKey`, `payload`) | 202 · 400 · 404 |

Reglas de validación: nombres obligatorios, máximo 255 caracteres, solo `[A-Za-z0-9._:-]`, sin el prefijo
reservado `amq.`; `ttlMs` y `maxMensajes` positivos; `deadLetterRoutingKey` exige `deadLetterExchange`;
los comodines `*` y `#` solo en exchanges topic; no se duplican recursos y no se eliminan los recursos de
GestorCitas (`app.admin.recursos-protegidos`).

```bash
# Ejemplos directos contra ms-rabbit-admin (en EC2-B o con port-forward)
curl -X POST localhost:8085/queues -H 'Content-Type: application/json' \
     -d '{"nombre":"q.demo","ttlMs":60000,"deadLetterExchange":"gestorcitas.dlx","deadLetterRoutingKey":"q.demo.dlq"}'
curl -X POST localhost:8085/queues -H 'Content-Type: application/json' -d '{"nombre":""}'   # 400 con detalle
curl -X DELETE localhost:8085/queues/q.notificaciones.citas                                 # 409 recurso protegido
curl -X DELETE localhost:8085/queues/q.demo                                                  # 204
```

## Ejecutar en local

```bash
cp .env.example .env            # completar RABBITMQ_PASS y RABBITMQ_ERLANG_COOKIE
docker compose up -d --build rabbit1 rabbit2
docker exec gestorcitas-rabbit1 rabbitmqctl cluster_status   # 2 nodos
docker compose up -d --build                                  # resto de servicios
```

Panel de RabbitMQ: <http://localhost:15672> (usuario y clave del `.env`).

## Guion de demostración

1. **Clúster**: `rabbitmqctl cluster_status` muestra `rabbit@rabbit1` y `rabbit@rabbit2`.
2. **Topología**: en el frontend, *Administración → Mensajería → Colas*: 3 colas, 3 colas `.retry` y 3 DLQ;
   en *Exchanges*: 2 topic, 1 direct de comprobantes y el direct `gestorcitas.dlx`.
3. **Flujo feliz**: un paciente reserva una cita → en los logs de `ms-notificaciones` aparecen `[CORREO]`,
   `[COMPROBANTE]` y `[ACK]`; el comprobante queda en el volumen `comprobantes`.
4. **Mensaje inválido → DLQ**: *Probar mensaje → "Evento incompleto"* → log `[DLQ] ... NO RECUPERABLE` y
   `q.notificaciones.citas.dlq` sube a 1.
5. **Reintentos → DLQ**: *Probar mensaje → "Correo caído"* → tres logs `[REINTENTO]` cada 5 s y luego `[DLQ] ... REINTENTOS AGOTADOS`.
6. **Administración**: crear la cola `q.demo`, el exchange `gestorcitas.demo` (topic) y el binding
   `gestorcitas.demo → q.demo` con `demo.*`; intentar crear una cola con nombre vacío (400) y eliminar una cola del sistema (409);
   eliminar `q.demo` (204).
7. **Desacople**: `docker stop gestorcitas-ms-notificaciones`, reservar otra cita (funciona igual) y ver el mensaje
   esperando en `q.notificaciones.citas`; al volver a iniciarlo, el mensaje se procesa.

```bash
docker logs -f gestorcitas-ms-notificaciones | grep -E '\[(ACK|REINTENTO|DLQ|CORREO|COMPROBANTE|AUDITORIA)\]'
```
