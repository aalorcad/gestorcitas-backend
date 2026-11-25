# 4. Despliegue en AWS: frontend en EC2-C, BFF en EC2-A y microservicios en EC2-B

> **EP5:** se agregan EC2-D y EC2-E (un nodo RabbitMQ cada una, repo `gestorcitas-rabbitmq`) y EC2-F (Kafka, repo `gestorcitas-kafka`).
> Ver la sección [4.9](#49-ep5-rabbitmq-y-kafka-en-sus-propias-ec2).

AWS API Gateway es el único punto público y entrega HTTPS (requisito de Entra ID para el login):
`/` sirve el frontend (EC2-C) y `/api/...` va al BFF (EC2-A). EC2-A y EC2-B clonan `gestorcitas-backend`;
EC2-C clona `gestorcitas-frontend`. En desarrollo el frontend también puede correr local (`npm run dev`).

## 4.1 Qué corre dónde

```
 Frontend local (http://localhost:5173)
        │ HTTPS + Bearer JWT
        ▼
 AWS API Gateway (HTTP API)
   25 rutas /api/... con autorizador JWT (1ª validación) + OPTIONS para CORS
        │ HTTP :8080
        ▼
 EC2-A  gestorcitas-bff   (IP elástica)          docker-compose.ec2-bff.yml
   BFF · Spring Security (2ª validación + roles)
        │ HTTP :8082-8084 por IP PRIVADA (MS_HOST)
        ▼
 EC2-B  gestorcitas-ms    (IP elástica + IP privada)   docker-compose.ec2-ms.yml
   ms-citas :8082 · ms-catalogo :8083 · ms-usuarios :8084
        │ TLS :1521
        ▼
 Oracle Autonomous Database (ACL: IP elástica de EC2-B)
```

| Recurso | Valor |
|---|---|
| Región | us-east-1 (Learner Lab) |
| AMI / tipo | Amazon Linux 2023 · t3.medium |
| Par de llaves | `gestorcitas-key` (`~/.ssh/gestorcitas-key.pem`, `chmod 400`) |
| User data | [`infra/aws/ec2-user-data.sh`](../infra/aws/ec2-user-data.sh) (Docker, Compose y Buildx) |

## 4.2 Security Groups

| SG | Instancia | Reglas de entrada |
|---|---|---|
| `gestorcitas-sg` | EC2-A (BFF) | SSH 22 desde *Mi IP* · TCP 8080 desde 0.0.0.0/0 (API Gateway llama por internet; sin token el BFF responde 401) |
| `gestorcitas-ms-sg` | EC2-B (microservicios) | SSH 22 desde *Mi IP* · TCP 8082-8088 **solo** desde el SG `gestorcitas-sg` (8085 ms-rabbit-admin, 8087 ms-kafka-admin, 8088 ms-analitica) |
| `gestorcitas-mq-sg` | EC2-D / EC2-E (RabbitMQ) | SSH 22 desde *Mi IP* · 5672 y 15672 desde `gestorcitas-ms-sg` · 4369 y 25672 desde el propio `gestorcitas-mq-sg` |
| `gestorcitas-kafka-sg` | EC2-F (Kafka) | SSH 22 y 8090 (Kafka UI) desde *Mi IP* · 9092-9094 desde `gestorcitas-ms-sg` |

> En otra red (por ejemplo la sala de clases) edita la regla SSH y vuelve a elegir *Mi IP*.

## 4.3 Crear las instancias

1. **EC2 → Lanzar instancia** (una vez por instancia): nombre `gestorcitas-bff` / `gestorcitas-ms`, Amazon Linux 2023, t3.medium, par `gestorcitas-key`, SG correspondiente, y en **Detalles avanzados → Datos de usuario** el contenido de `infra/aws/ec2-user-data.sh`.
2. **Direcciones IP elásticas → Asignar** (una por instancia) → **Asociar** a su instancia. No se liberan: API Gateway y la ACL de Oracle dependen de ellas.
3. Anota la **IP privada** de EC2-B (`172.31.x.x`): va en `MS_HOST` del `.env`.
4. En Oracle Cloud agrega la IP elástica de **EC2-B** a la ACL de la Autonomous Database.

Comprobar Docker en cada instancia:

```bash
ssh -i ~/.ssh/gestorcitas-key.pem ec2-user@<IP-elástica>
docker --version && docker compose version && docker buildx version
```

## 4.4 Desplegar EC2-B (microservicios)

Desde tu Mac copia el `.env` (nunca va a GitHub) y entra:

```bash
scp -i ~/.ssh/gestorcitas-key.pem .env ec2-user@<IP-EC2-B>:~/
ssh -i ~/.ssh/gestorcitas-key.pem ec2-user@<IP-EC2-B>
```

En EC2-B:

```bash
git clone https://github.com/aalorcad/gestorcitas-backend.git
mv ~/.env gestorcitas-backend/ && cd gestorcitas-backend
docker compose -f docker-compose.ec2-ms.yml up -d --build
curl -s localhost:8083/especialidades      # JSON de especialidades desde Oracle
```

## 4.5 Desplegar EC2-A (BFF)

El `.env` debe tener `MS_HOST=<IP privada de EC2-B>`.

```bash
scp -i ~/.ssh/gestorcitas-key.pem .env ec2-user@<IP-EC2-A>:~/
ssh -i ~/.ssh/gestorcitas-key.pem ec2-user@<IP-EC2-A>
git clone https://github.com/aalorcad/gestorcitas-backend.git
mv ~/.env gestorcitas-backend/ && cd gestorcitas-backend
curl -s <IP-privada-EC2-B>:8083/especialidades   # verifica el Security Group
docker compose -f docker-compose.ec2-bff.yml up -d --build
curl -i localhost:8080/api/me                     # 401: Spring Security exige token
```

## 4.6 API Gateway

En **AWS CloudShell** (ícono `>_` de la consola, región us-east-1):

```bash
git clone https://github.com/aalorcad/gestorcitas-backend.git && cd gestorcitas-backend
export AWS_REGION=us-east-1
export ENTRA_TENANT_ID=<tenant-id>
export ENTRA_API_CLIENT_ID=<client-id de gestorcitas-api>
export BFF_HOST=ec2-<IP-EC2-A-con-guiones>.compute-1.amazonaws.com
# export API_ID=<id>   # para reutilizar una API existente y conservar su URL
bash infra/aws/api-gateway.sh
```

El script crea (o reemplaza) el autorizador JWT (issuer + audience + scope `access_as_user`), la integración
con el BFF, las 25 rutas protegidas, la ruta `OPTIONS` y el CORS para `http://localhost:5173`.

Luego, en tu Mac, `frontend/.env` → `VITE_API_BASE_URL=<URL de API Gateway>` y `npm run dev`.

Verificación:

```bash
curl -i <URL>/api/me          # 401 (sin token)
curl -i <URL>/api/no-existe   # 401 sin token; con token el BFF responde 403
```

Evidencia completa con token: colección Postman `infra/postman/GestorCitas.postman_collection.json`.

## 4.6b Frontend en EC2-C (`gestorcitas-front`)

1. **Security Group** `gestorcitas-front-sg`: SSH 22 desde *Mi IP* · HTTP 80 desde 0.0.0.0/0 (API Gateway llama por internet).
2. **Lanzar instancia** `gestorcitas-front`: Amazon Linux 2023, **t3.small** (el build de Vite necesita ~1 GB de RAM), par `gestorcitas-key`, ese SG y el mismo user data. Asígnale una **IP elástica**.
3. Desde tu Mac copia `frontend/.env` (Client ID, Tenant ID, scope y `VITE_API_BASE_URL` = URL de API Gateway):

```bash
scp -i ~/.ssh/gestorcitas-key.pem frontend/.env ec2-user@<IP-EC2-C>:~/
ssh -i ~/.ssh/gestorcitas-key.pem ec2-user@<IP-EC2-C>
git clone https://github.com/aalorcad/gestorcitas-frontend.git
mv ~/.env gestorcitas-frontend/ && cd gestorcitas-frontend
docker compose up -d --build
curl -s localhost | head -5          # HTML de la SPA
```

4. **API Gateway**: vuelve a ejecutar `infra/aws/api-gateway.sh` agregando `export FRONT_HOST=<DNS público de EC2-C>`
   (crea `GET /` y `GET /{proxy+}` hacia nginx, sin autorizador).
5. **Entra ID** → `gestorcitas-frontend` → **Autenticación** → SPA: agrega `https://<id>.execute-api.us-east-1.amazonaws.com` y `…/login`.
6. **EC2-A** `.env`: `CORS_ALLOWED_ORIGINS=http://localhost:5173,https://<id>.execute-api.us-east-1.amazonaws.com` y
   `docker compose -f docker-compose.ec2-bff.yml up -d` (el navegador envía `Origin` también en peticiones del mismo dominio).
7. Abre `https://<id>.execute-api.us-east-1.amazonaws.com` e inicia sesión.

## 4.7 Actualizar después de un cambio

```bash
# Mac
git push
# EC2-A (BFF) o EC2-B (microservicios)
cd ~/gestorcitas-backend && git pull
docker compose -f docker-compose.ec2-bff.yml build --no-cache bff     # en EC2-A
docker compose -f docker-compose.ec2-bff.yml up -d --force-recreate
docker compose -f docker-compose.ec2-ms.yml up -d --build            # en EC2-B
```

Logs: `docker logs --tail 40 gestorcitas-bff` (EC2-A) · `docker logs --tail 40 gestorcitas-ms-citas` (EC2-B).

## 4.8 Detener y volver a iniciar (Learner Lab)

- **Detener:** EC2 → selecciona ambas → *Estado de la instancia → Detener*. Nunca *Terminar* ni liberar las IP elásticas.
- **Iniciar:** *Start Lab* → EC2 → *Iniciar instancia*. Las IP no cambian y los contenedores arrancan solos (`restart: unless-stopped`).
- API Gateway no se apaga ni cobra sin peticiones. Oracle Always Free se detiene tras 7 días sin uso: iníciala desde OCI.

## 4.9 EP5: RabbitMQ y Kafka en sus propias EC2

| EC2 | Nombre | Tipo | Repositorio | Comando |
|---|---|---|---|---|
| D | gestorcitas-mq1 | t3.small | gestorcitas-rabbitmq | `docker compose -f docker-compose.ec2.yml up -d` (`.env`: `RABBIT_NODE=rabbit1`, `RABBIT_PEER=rabbit2`, `RABBIT_PEER_HOST=<IP privada de EC2-E>`) |
| E | gestorcitas-mq2 | t3.small | gestorcitas-rabbitmq | igual, con `rabbit2` / `rabbit1` / IP privada de EC2-D |
| F | gestorcitas-kafka | t3.large · 20 GB | gestorcitas-kafka | `docker compose up -d` (`.env`: `KAFKA_ADVERTISED_HOST=<IP privada de EC2-F>`) |

1. Con ambos nodos arriba, en EC2-D: `bash scripts/politica-ha.sh gestorcitas-rabbit1` (replica las colas en los 2 nodos).
2. En EC2-F: `docker logs gestorcitas-kafka-init` muestra los 4 tópicos con 3 particiones y RF 3. Kafka UI en `http://<IP pública EC2-F>:8090`.
3. En EC2-B, agregar al `.env`: `MQ1_HOST`, `MQ2_HOST`, `KAFKA_HOST` (IPs privadas), `RABBITMQ_USER/PASS` (los mismos de los nodos),
   `NOTIFICACIONES_DB_*`, `ANALITICA_DB_*` y `JAVA_XMX=256m`; luego
   `docker compose -f docker-compose.ec2-ms.yml up -d --build --remove-orphans`.
4. En EC2-A y EC2-C: `git pull` y `docker compose ... up -d --build` (el BFF toma `KAFKA_ADMIN_URL` y `ANALITICA_URL` de `MS_HOST`).

Las IPs privadas no cambian al detener las instancias; las públicas sí (solo se usan para SSH y Kafka UI).
