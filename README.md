# GestorCitaCloudNative

Sistema de **reserva de citas médicas** con arquitectura cloud native:
React + MSAL (Microsoft Entra ID) → AWS API Gateway (JWT) → BFF Spring Security → microservicios Spring Boot → Oracle Autonomous Database,
con mensajería asíncrona sobre un clúster RabbitMQ de 2 nodos (colas, exchanges y DLQ, [docs/05-mensajeria-rabbitmq.md](docs/05-mensajeria-rabbitmq.md))
y **streaming de eventos con Apache Kafka** (3 Zookeeper, 3 brokers, analítica en tiempo real; ver [Streaming con Kafka](#streaming-con-kafka-ep5)).

El sistema está repartido en **4 repositorios**:

| Repositorio | Contenido |
|---|---|
| **gestorcitas-backend** (este) | BFF y microservicios Spring Boot (productores y consumidores de RabbitMQ y Kafka) |
| [gestorcitas-frontend](https://github.com/aalorcad/gestorcitas-frontend) | React + MSAL |
| [gestorcitas-rabbitmq](https://github.com/aalorcad/gestorcitas-rabbitmq) | Clúster RabbitMQ de 2 nodos (Docker Compose local y por EC2) |
| [gestorcitas-kafka](https://github.com/aalorcad/gestorcitas-kafka) | Clúster Kafka: 3 Zookeeper + 3 brokers + Kafka UI + tópicos |

## Arquitectura

```
┌──────────────────────┐   login / tokens    ┌──────────────────────┐
│  React + MSAL (SPA)  │ ◄─────────────────► │  Microsoft Entra ID  │
│  3 portales por rol  │                     │  App Regs · Roles    │
└──────────┬───────────┘                     └──────────────────────┘
           │ HTTPS  Authorization: Bearer <JWT>
           ▼
┌──────────────────────────────────────────┐
│ AWS API Gateway (HTTP API)               │  ← único punto público
│ · JWT authorizer (iss, aud, scp, firma)  │  ← 1ª validación
│ · 25 rutas /api/... + CORS (preflight)   │
└──────────┬───────────────────────────────┘
           ▼
┌──────────────────────────────────────────┐
│ BFF · Spring Boot + Spring Security :8080│  ← 2ª validación JWT
│ · autorización por App Role              │  ← orquesta y agrega datos
│ · propaga identidad (X-User-*)           │
└───┬──────────────────┬──────────────┬────┘
    ▼  red privada     ▼              ▼
┌────────────┐   ┌─────────────┐   ┌─────────────┐
│ ms-citas   │──►│ ms-usuarios │──►│ ms-catalogo │
│ :8082      │   │ :8084       │   │ :8083       │
│ (principal)│   │ pac/méd/adm │   │ especialid. │
└─────┬──────┘   └──────┬──────┘   └──────┬──────┘
      └────────── TLS ──┼─────────────────┘
                        ▼
        ┌────────────────────────────────────┐
        │ Oracle Autonomous Database (OCI)   │  1 instancia
        │ esquemas: CITAS · USUARIOS · CATALOGO │  1 esquema por microservicio
        └────────────────────────────────────┘

  ms-citas · ms-usuarios ──publican──► ┌──────────────────────────────┐ ──consume──► ms-notificaciones :8086
  (después del commit)                 │ RabbitMQ · clúster 2 nodos   │              (correo, comprobante, auditoría,
  BFF /api/admin/mensajeria ─────────► │ topic · direct · DLX · DLQ   │               reintentos y DLQ)
            └──► ms-rabbit-admin :8085 └──────────────────────────────┘

  ms-citas ────► citas.eventos ───┐    ┌──────────────────────────────┐ ──grupo analitica───────► ms-analitica :8088 (×2)
  ms-usuarios ─► usuarios.estado ─┴──► │ Kafka · 3 Zookeeper          │                            indicadores en tiempo real
  (después del commit, acks=all)       │ 3 brokers · RF 3 · min ISR 2 │ ──grupo historial-citas─► ms-notificaciones
  BFF /api/admin/kafka ─► ms-kafka-admin :8087 (AdminClient)          │    errores ──► *.DLT
                                       └──────────────────────────────┘
```

### Dónde corre cada componente

| Componente | Despliegue | Público |
|---|---|---|
| Frontend React | **EC2-C** `gestorcitas-front` · nginx :80 (en desarrollo: `npm run dev`) | Vía API Gateway `GET /` y `GET /{proxy+}` |
| API Gateway | AWS HTTP API · frontend + 25 rutas `/api` con autorizador JWT | Sí, HTTPS (único punto público) |
| BFF | **EC2-A** `gestorcitas-bff` · contenedor :8080 | Solo vía API Gateway |
| ms-citas · ms-usuarios · ms-catalogo · ms-rabbit-admin · ms-kafka-admin · ms-analitica · ms-notificaciones | **EC2-B** `gestorcitas-ms` (t3.large) · :8082 · :8084 · :8083 · :8085 · :8087 · :8088 | No: el Security Group solo acepta al BFF (8082-8088) |
| RabbitMQ rabbit1 · rabbit2 | **EC2-D** `gestorcitas-mq1` · **EC2-E** `gestorcitas-mq2` (repo gestorcitas-rabbitmq) | No: 5672/15672 solo desde EC2-B |
| Kafka: 3 Zookeeper + 3 brokers + Kafka UI | **EC2-F** `gestorcitas-kafka` (t3.large, repo gestorcitas-kafka) | No: 9092-9094 solo desde EC2-B; Kafka UI :8090 solo desde la IP del administrador |
| Base de datos | Oracle Autonomous Database (OCI) | Solo la IP de EC2-B (ACL) |
| Identidad | Microsoft Entra ID | — |

La aplicación completa se abre en la URL HTTPS de API Gateway (`https://…execute-api…amazonaws.com`): `/` sirve el frontend y `/api/…` va al BFF. Despliegue en [docs/04-despliegue-ec2.md](docs/04-despliegue-ec2.md).

| Servicio | Responsabilidad |
|---|---|
| **ms-citas** (principal) | Reserva, cancelación, confirmación y atención de citas; disponibilidad horaria; indicadores |
| **ms-usuarios** | Pacientes, médicos y administradores: sincronización con Entra ID, perfiles, activación, pre-registro de médicos |
| **ms-catalogo** | Especialidades médicas y valor de la consulta |
| **bff** | Segunda validación JWT, autorización por rol, orquestación y reglas entre servicios |
| **ms-notificaciones** | Consumidor RabbitMQ: notifica cambios de citas, genera comprobantes y audita usuarios (ACK manual, reintentos, DLQ) |
| **ms-rabbit-admin** | API REST para crear y eliminar colas, exchanges y bindings (rol Admin vía BFF) |
| **ms-analitica** (EP5) | Consumidor Kafka (grupo `analitica`, 2 instancias): indicadores en tiempo real en Oracle (esquema ANALITICA) |
| **ms-kafka-admin** (EP5) | API REST con AdminClient: clúster, tópicos (crear, configurar, particiones) y grupos de consumidores (lag, reinicio de offsets) |

## Actores y vistas

Cada rol tiene su **propio portal** con inicio, menú lateral y pantallas:

| Rol (App Role) | Portal | Pantallas |
|---|---|---|
| **Paciente** | `/paciente` | Inicio (indicadores + próximas citas), **Mi perfil** (RUT, fecha nac., previsión, teléfono), Reservar cita (especialidad → médico → fecha → bloque), Mis citas (cancelar) |
| **Medico** | `/medico` | Inicio (citas de hoy, por confirmar, atendidas), Mi agenda (confirmar, atender con observaciones), **Mi perfil** (especialidad, registro SIS, teléfono, presentación) |
| **Admin** | `/admin` | **Panel** (KPIs de usuarios, citas y catálogo), **Usuarios** (filtrar por rol, buscar, activar/desactivar, registrar y editar médicos), Especialidades (CRUD + valor), Citas (filtrar, cancelar), **Mensajería** (RabbitMQ), **Streaming (Kafka)** (clúster, tópicos, grupos y lag), **Analítica** (indicadores en tiempo real) |
| Todos | `/sesion` | Claims del token validados por el BFF |

Al entrar a `/` se redirige al portal del rol principal (Admin > Médico > Paciente). Un usuario con varios roles ve todas sus secciones en el menú.

### Ciclo de vida del usuario

1. El usuario inicia sesión con Entra ID. El frontend llama `POST /api/usuarios/me/sincronizar`.
2. **ms-usuarios** busca por `oid`. Si no existe, busca por email: si un Admin **pre-registró a ese médico**, se vincula su cuenta; si no, se crea el usuario.
3. Los roles se toman del token (Entra ID es la fuente de verdad). Se crea el perfil de Paciente o de Médico si falta.
4. Si el Admin desactivó la cuenta, el frontend bloquea el acceso y ms-citas rechaza las reservas.

## Estructura del repositorio

```
GestorCitaCloudNative/
├── frontend/          React 18 + Vite + TypeScript + MSAL (Dockerfile + nginx.conf)
├── bff/               Backend For Frontend (Spring Security)
├── ms-citas/          Microservicio principal
├── ms-usuarios/       Microservicio de usuarios
├── ms-catalogo/       Microservicio de catálogo
├── ms-notificaciones/ Consumidor RabbitMQ (notificaciones, comprobantes, auditoría)
├── ms-rabbit-admin/   Administrador de RabbitMQ (API REST)
├── ms-analitica/      Consumidor Kafka: indicadores en tiempo real (EP5)
├── ms-kafka-admin/    Administrador de Kafka con AdminClient (EP5)
├── infra/
│   ├── db/oracle/     Scripts de esquemas para Autonomous DB (adb/) y Oracle local (local/)
│   ├── postman/       Colección de pruebas de API Gateway
│   └── aws/           Script API Gateway + user-data EC2
├── docs/              00-guia-paso-a-paso.md · 01-entra-id.md · 01b-entra-external-id.md · 03-oracle-autonomous-db.md · 04-despliegue-ec2.md · 05-mensajeria-rabbitmq.md
├── docker-compose.yml          Entorno local completo (se une a las redes de RabbitMQ y Kafka)
├── docker-compose.ec2-*.yml    Un archivo por EC2 (bff, ms)
├── mvnw               Maven Wrapper (no requiere Maven instalado)
└── pom.xml            Agregador Maven
```

### Backend: capas

```
cl.duoc.gestorcitas.<servicio>
├── entity/        Entidades JPA
├── repository/    Spring Data JPA
├── service/       Interfaces + impl/  ← TODA la lógica de negocio
├── controller/    REST: recibe, valida formato (@Valid) y delega
├── dto/  mapper/  client/  config/  exception/  util/
├── messaging/     RabbitMQ: config/ (nombres + beans), producer/, consumer/<dominio>/, event/ (contratos JSON)
│                  Kafka: kafka/ (productor, propiedades, eventos) o consumer/ + config/ (manejo de errores y DLT)
└── security/      (solo BFF) validadores JWT y conversor de roles
```

**Reglas de negocio:**

- **ms-citas** (`CitaServiceImpl`):
  - Días hábiles, 08:00–18:00, bloques de 30 min; la cita debe ser futura.
  - El paciente debe estar registrado, activo y con perfil completo (consulta a ms-usuarios).
  - El médico debe estar activo y con especialidad asignada (ms-usuarios).
  - No se reserva dos veces el mismo bloque, ni para el médico ni para el paciente; máximo 1 cita activa por especialidad por paciente.
  - Solo cancela el dueño o un Admin (citas activas y futuras); solo el médico asignado confirma o atiende.
- **ms-usuarios** (`UsuarioServiceImpl`, `MedicoServiceImpl`):
  - Sincronización y vinculación con Entra ID.
  - RUT válido (módulo 11) y único; fecha de nacimiento coherente.
  - Email único al pre-registrar médicos; la especialidad debe existir y estar activa (ms-catalogo).
  - Un Admin no puede desactivarse a sí mismo y siempre debe quedar al menos un Admin activo.
- **ms-catalogo**: nombre de especialidad único, valor de consulta entre 0 y $1.000.000.
- **BFF** (reglas entre servicios): no se desactiva una especialidad con médicos activos; el dashboard agrega los datos de los 3 microservicios.

### Frontend: estructura

```
src/
├── app/                 router.tsx, layouts/ (MainLayout, navigation), providers/, routes/{paciente,medico,admin}/
├── assets/
├── components/ui/       Button, Card, Modal, Input, Select, Tabs, StatTile… (sin lógica de negocio)
├── features/
│   ├── auth/            MSAL: AuthGuard, RoleGuard, login/logout, roles del token
│   ├── usuarios/        UsuarioGate, perfiles, MedicoPicker, UsuariosAdmin, DashboardAdmin
│   ├── catalogo/        Especialidades
│   ├── citas/           Reserva, mis citas, agenda, administración de citas
│   ├── mensajeria/      Administración de RabbitMQ
│   ├── kafka/           Administración de Kafka (clúster, tópicos, grupos y lag)
│   └── analitica/       Indicadores en tiempo real
├── hooks/  services/ (msal + http con MsalInterceptor)  stores/  types/  utils/
```

Guards en cadena (`app/router.tsx`): **AuthGuard** (sesión Entra ID) → **UsuarioGate** (sincronizado y activo) → **RoleGuard** (rol del portal).

## Streaming con Kafka (EP5)

Kafka **complementa** a RabbitMQ sin modificarlo: RabbitMQ sigue a cargo de los comandos puntuales (correo, comprobante,
auditoría, con reintentos y DLQ) y Kafka mantiene un **log de eventos** que varios grupos leen de forma independiente y pueden releer.
El clúster (3 Zookeeper, 3 brokers, Kafka UI y creación de tópicos) está en el repositorio
[gestorcitas-kafka](https://github.com/aalorcad/gestorcitas-kafka).

### Tópicos

| Tópico | Part. | RF | Limpieza | Retención | Clave | Propósito |
|---|---|---|---|---|---|---|
| `gestorcitas.citas.eventos` | 3 | 3 | delete | 7 días | `citaId` | Cambios de estado de cada cita; se relee para recalcular indicadores |
| `gestorcitas.usuarios.estado` | 3 | 3 | compact | último valor por clave (tombstones 1 día) | `usuarioId` | Estado actual de cada usuario |
| `gestorcitas.citas.eventos.DLT` · `gestorcitas.usuarios.estado.DLT` | 3 | 3 | delete | 14 días | — | Eventos que fallaron, para investigarlos |

`min.insync.replicas=2` en todos y productores con `acks=all` e idempotencia: un evento se confirma cuando está en al menos 2 brokers.

### Productores y consumidores

| Servicio | Rol | Tópico | Grupo | Qué hace |
|---|---|---|---|---|
| ms-citas | Productor (`KafkaTemplate`) | citas.eventos | — | Publica CREADA, CONFIRMADA, CANCELADA y ATENDIDA **después del commit** |
| ms-usuarios | Productor | usuarios.estado | — | Publica el estado completo al registrar, activar o desactivar |
| ms-analitica (×2) | Consumidor (`@KafkaListener`) | ambos | `analitica` | Guarda eventos y estado de usuarios en Oracle y expone `GET /analitica/resumen` y `/eventos` |
| ms-notificaciones | Consumidor | citas.eventos | `historial-citas` | Historial de estados de cada cita en Oracle (tabla `historial_citas`) |

Nombres de tópicos, grupos, reintentos y sufijo de DLT están en `app.kafka` de cada `application.yml`; ninguno está escrito en el código.

### Estructura de los eventos (JSON)

**`CitaEventoKafka`** (clave = `citaId`). Productor: `ms-citas/.../messaging/kafka/CitaEventoKafka.java`.

| Campo | Tipo | Para qué sirve |
|---|---|---|
| `eventoId` | UUID | Identificador único: el consumidor descarta duplicados (entrega at-least-once) |
| `tipo` | CREADA · CONFIRMADA · CANCELADA · ATENDIDA | Qué ocurrió con la cita |
| `citaId` | número | Cita afectada; también es la clave (orden por cita) |
| `pacienteId` | texto (oid) | Paciente dueño de la cita |
| `medicoId` · `medicoNombre` | número · texto | Agrupar indicadores por médico sin consultar ms-usuarios |
| `especialidadId` · `especialidadNombre` | número · texto | Agrupar indicadores por especialidad |
| `fechaHora` · `estado` | fecha · texto | Datos de la cita después del cambio |
| `ocurridoEn` | fecha | Momento del cambio |

**`UsuarioEstadoKafka`** (clave = `usuarioId`). Productor: `ms-usuarios/.../messaging/kafka/UsuarioEstadoKafka.java`.

| Campo | Tipo | Para qué sirve |
|---|---|---|
| `eventoId` | UUID | Descartar duplicados |
| `tipo` | REGISTRADO · ACTIVADO · DESACTIVADO | Cambio que originó la publicación |
| `usuarioId` · `email` · `nombre` | número · texto | Identidad del usuario |
| `roles` · `activo` | lista · booleano | **Estado completo**: con la compactación solo queda el último registro de cada clave |
| `actualizadoEn` | fecha | Descartar estados más antiguos que el guardado |

Los consumidores validan cada evento con Bean Validation (`CitaEventoMsg`, `UsuarioEstadoMsg`, `CitaEventoKafkaMsg`).

### Manejo de errores

- `ErrorHandlingDeserializer`: un JSON corrupto no bloquea la partición.
- `DefaultErrorHandler`: 3 intentos ante errores transitorios y luego `DeadLetterPublishingRecoverer` publica en `<tópico>.DLT`
  (misma partición, con la causa en las cabeceras). Los eventos inválidos van directo, sin reintentos.
- Idempotencia por `eventoId` (restricción única en Oracle) y logs con tópico, partición, offset, clave y eventoId.

### Supervisión

```bash
docker exec gestorcitas-kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --describe --group analitica
```
También en Kafka UI (pestaña *Consumers*) y en la app: **Administración → Streaming (Kafka) → Grupos de consumidores → Detalle y lag**.

## Endpoints públicos (vía API Gateway → BFF)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/me` | autenticado (claims del token) |
| POST | `/api/usuarios/me/sincronizar` | autenticado |
| GET | `/api/usuarios/me` | autenticado |
| PUT | `/api/usuarios/me/perfil-paciente` | Paciente |
| PUT | `/api/usuarios/me/perfil-medico` | Medico |
| GET | `/api/usuarios/medicos?especialidadId=` | autenticado |
| GET | `/api/usuarios?rol=&q=` | Admin |
| PATCH | `/api/usuarios/{id}/activar` · `/desactivar` | Admin |
| POST | `/api/usuarios/medicos` | Admin |
| PUT | `/api/usuarios/{id}/perfil-medico` | Admin |
| GET | `/api/admin/dashboard` | Admin |
| GET | `/api/catalogo/especialidades` | autenticado |
| POST/PUT/PATCH/DELETE | `/api/catalogo/**` | Admin |
| GET | `/api/citas/disponibilidad?medicoId=&fecha=` | autenticado |
| POST | `/api/citas` | Paciente |
| GET | `/api/citas/mias` | Paciente |
| PATCH | `/api/citas/{id}/cancelar` | Paciente (dueño) / Admin |
| GET | `/api/citas/agenda?fecha=` | Medico |
| PATCH | `/api/citas/{id}/confirmar` · `/atender` | Medico (asignado) |
| GET | `/api/citas?estado=` | Admin |
| GET/POST/DELETE | `/api/admin/mensajeria/**` | Admin (RabbitMQ) |
| GET/POST/PATCH/DELETE | `/api/admin/kafka/**` | Admin (Kafka: `cluster`, `topics`, `consumer-groups`) |
| GET | `/api/analitica/resumen` · `/api/analitica/eventos?limite=` | Admin |

## Ejecutar en local

> 📘 **Guía paso a paso (Entra ID → local → EC2 → API Gateway):** [docs/00-guia-paso-a-paso.md](docs/00-guia-paso-a-paso.md)

Requisitos: **Java 17+**, Node 20+, Docker. No necesitas Maven: usa `./mvnw`.

1. Configura Entra ID siguiendo **[docs/01-entra-id.md](docs/01-entra-id.md)**. Para que los pacientes **creen su cuenta** desde el frontend usa Entra External ID: **[docs/01b-entra-external-id.md](docs/01b-entra-external-id.md)**.
2. Crea la Autonomous Database y sus esquemas siguiendo **[docs/03-oracle-autonomous-db.md](docs/03-oracle-autonomous-db.md)**
   (EP5: además ejecuta `infra/db/oracle/adb/03-esquemas-ep5.sql` para NOTIFICACIONES y ANALITICA).
3. Brokers y backend (los repos de RabbitMQ y Kafka se clonan dentro de esta carpeta; están en `.gitignore`):
   ```bash
   git clone https://github.com/aalorcad/gestorcitas-rabbitmq.git
   git clone https://github.com/aalorcad/gestorcitas-kafka.git
   (cd gestorcitas-rabbitmq && cp .env.example .env && docker compose up -d)   # completar claves en .env
   (cd gestorcitas-kafka && cp .env.example .env && docker compose up -d)      # Kafka UI: http://localhost:8090
   cp .env.example .env        # DB_URL + esquemas Oracle, Tenant ID, Client ID de la API, RabbitMQ, email del médico demo
   docker compose up -d --build
   ```
   Sin Autonomous DB todavía: pon `DB_URL=jdbc:oracle:thin:@//oracle-local:1521/FREEPDB1` y usa `docker compose --profile oracle-local up -d --build` (Oracle Free en Docker con los mismos esquemas).

   Sin Docker: ejecuta en cada carpeta `../mvnw spring-boot:run` (orden: ms-catalogo, ms-usuarios, ms-citas, bff), exportando `DB_URL`, `DB_USER` y `DB_PASSWORD` del esquema de cada servicio.
4. Frontend:
   ```bash
   cd frontend
   cp .env.example .env        # VITE_API_BASE_URL=http://localhost:8080 en local
   npm install
   npm run dev                 # http://localhost:5173
   ```
5. Despliegue en AWS (frontend en EC2-C, BFF en EC2-A, microservicios en EC2-B, detrás de API Gateway): **[docs/04-despliegue-ec2.md](docs/04-despliegue-ec2.md)**.
   EP5: RabbitMQ en EC2-D/E (`docker-compose.ec2.yml` del repo gestorcitas-rabbitmq) y Kafka en EC2-F (repo gestorcitas-kafka);
   EC2-B necesita en su `.env` `MQ1_HOST`, `MQ2_HOST` y `KAFKA_HOST` (IPs privadas), ver `.env.example`.

### Datos de demostración

- **Especialidades:** Medicina General, Pediatría, Cardiología y Dermatología (con valor de consulta).
- **Médicos pre-registrados:** Dr. Elian Barra (`DEMO_MEDICO_EMAIL`), Dr. Matías Soto y Dr. Felipe Muñoz. Cuando `medico1` inicie sesión en Entra ID, queda vinculado al Dr. Elian Barra.
- **Pacientes y admins:** se crean solos en su primer login, según el App Role asignado.

## Tests

**Evidencia de rutas (Postman):** `infra/postman/GestorCitas.postman_collection.json` prueba las 25 rutas de API Gateway
con y sin token: 401 (sin token o token inválido, API Gateway), 200/201/204 (JSON de los microservicios),
403 (rol no permitido o ruta no definida, BFF). Instrucciones en la descripción de la colección.


```bash
./mvnw clean verify        # reglas de negocio (Mockito), JPA (H2), seguridad del BFF (MockMvc) y Kafka embebido:
                           # consumo, duplicados, DLT, tópico compactado y API de ms-kafka-admin
cd frontend && npm run typecheck
```

## Checklist del alcance

- [x] Entra ID: tenant, usuarios de prueba, App Roles
- [x] App Registration frontend (SPA) y API (scope `access_as_user` + roles)
- [x] Redirect URI, Client ID, Tenant ID, Authority y scopes
- [x] React + MSAL: login y logout
- [x] Rutas protegidas con Guards (AuthGuard, UsuarioGate, RoleGuard)
- [x] MsalInterceptor adjuntando `Authorization: Bearer`
- [x] API Gateway como único punto público, validación JWT, CORS y ruta OPTIONS
- [x] BFF con Spring Security y segunda validación JWT
- [x] Microservicio principal (ms-citas), de catálogo (ms-catalogo) y de usuarios (ms-usuarios)
- [x] Persistencia en Oracle Autonomous Database: 1 instancia, esquemas CATALOGO · USUARIOS · CITAS
- [x] Portales diferenciados y funcionales para Paciente, Médico y Admin
- [x] RabbitMQ: nombres centralizados en `app.messaging`, beans Queue/Exchange/Binding por caso de uso
- [x] Productores (ms-citas, ms-usuarios) y consumidores `@RabbitListener` por dominio (ms-notificaciones)
- [x] ACK manual, reintentos con cola de espera (TTL) y DLQ con registro en logs
- [x] ms-rabbit-admin: API REST validada para colas, exchanges y bindings (`RabbitAdminService`)
- [x] RabbitMQ en 2 EC2 (un nodo cada una) con política HA y persistencia en volúmenes; ms-notificaciones guarda lo procesado en Oracle
- [x] Kafka: 3 Zookeeper + 3 brokers + Kafka UI con Docker Compose en AWS (repo gestorcitas-kafka)
- [x] Dos tópicos con 3 particiones y 3 réplicas, retención definida y tópicos DLT
- [x] Productores con clave, JSON y acks=all (ms-citas, ms-usuarios); consumidores `@KafkaListener` con grupos `analitica` e `historial-citas`
- [x] Manejo explícito de errores (reintentos acotados, DLT, idempotencia) y reacción en tiempo real (vista Analítica)
- [x] ms-kafka-admin: administración de tópicos y grupos de consumidores (lag, reinicio de offsets)
