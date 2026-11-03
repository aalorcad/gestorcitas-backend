# Clúster RabbitMQ (2 nodos)

Lo levantan `docker-compose.yml` (local) y `docker-compose.ec2-ms.yml` (EC2-B) con los servicios
`rabbit1` y `rabbit2`, ambos con la imagen `rabbitmq:3.13-management`.

| Archivo | Uso |
|---|---|
| `cluster.conf` | Descubrimiento de pares `classic_config` (rabbit@rabbit1, rabbit@rabbit2) |

Variables del `.env` (ver `.env.example`):

| Variable | Descripción |
|---|---|
| `RABBITMQ_USER` / `RABBITMQ_PASS` | Usuario de la aplicación y del panel web |
| `RABBITMQ_ERLANG_COOKIE` | Secreto compartido por los nodos para formar el clúster (igual en ambos) |

## Verificar el clúster

```bash
docker exec gestorcitas-rabbit1 rabbitmqctl cluster_status   # deben aparecer rabbit@rabbit1 y rabbit@rabbit2
```

Panel web: `http://localhost:15672` (nodo 1) y `http://localhost:15673` (nodo 2).

## Topología

La declara `ms-notificaciones` al conectarse (ver `RabbitConfig` y `app.messaging` en su `application.yml`):

| Flujo | Exchange (tipo) | Routing key | Cola | Reintento (TTL) | DLQ |
|---|---|---|---|---|---|
| Notificación de citas | `gestorcitas.citas.eventos` (topic) | `cita.*` | `q.notificaciones.citas` | `q.notificaciones.citas.retry` | `q.notificaciones.citas.dlq` |
| Comprobante de cita | `gestorcitas.citas.comprobantes` (direct) | `comprobante.generar` | `q.comprobantes.citas` | `q.comprobantes.citas.retry` | `q.comprobantes.citas.dlq` |
| Auditoría de usuarios | `gestorcitas.usuarios.eventos` (topic) | `usuario.#` | `q.auditoria.usuarios` | `q.auditoria.usuarios.retry` | `q.auditoria.usuarios.dlq` |

Todas las DLQ reciben los mensajes a través del exchange direct `gestorcitas.dlx`.

> Las colas son *classic*: en un clúster de 2 nodos cada cola vive en el nodo donde se declaró, pero los
> clientes pueden conectarse a cualquiera de los dos. Las colas *quorum* no se usan porque con 2 nodos
> la caída de uno deja la cola sin mayoría.
