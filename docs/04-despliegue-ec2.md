# 4. Despliegue en AWS: BFF en EC2-A y microservicios en EC2-B

El frontend corre **local** (`npm run dev`) y consume la API por AWS API Gateway, que es el único punto público.
Ambas EC2 clonan el mismo repositorio `gestorcitas-backend` y cada una levanta solo su parte.

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
| `gestorcitas-ms-sg` | EC2-B (microservicios) | SSH 22 desde *Mi IP* · TCP 8082-8084 **solo** desde el SG `gestorcitas-sg` |

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
curl -i <URL>/api/no-existe   # 404 (ruta no publicada)
```

Evidencia completa con token: colección Postman `infra/postman/GestorCitas.postman_collection.json`.

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
