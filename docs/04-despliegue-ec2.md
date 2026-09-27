# 4. Despliegue en AWS: frontend, BFF y microservicios en EC2

## 4.1 Qué corre dónde

```
                               Internet (solo HTTPS)
  Navegador ─────────────► https://abc123.execute-api.us-east-1.amazonaws.com
                           ┌──────────────────────────────────────────────────┐
                           │ AWS API Gateway (HTTP API) · único punto público │
                           │                                                  │
                           │  GET /  y  GET /{proxy+}      ANY /api/{proxy+}   │
                           │  (sin autorizador)            + autorizador JWT   │
                           │                               (1ª validación)     │
                           │                               OPTIONS /api/...    │
                           └──────────┬──────────────────────────┬────────────┘
                                      │ HTTP :80                 │ HTTP :8080
                  ┌───────────────────▼──────────────────────────▼───────────────┐
                  │ EC2 · Amazon Linux 2023 · Docker Compose                     │
                  │                                                              │
                  │  ┌──────────────────┐        ┌───────────────────────────┐   │
                  │  │ frontend         │        │ bff                       │   │
                  │  │ React + nginx    │        │ Spring Security           │   │
                  │  │ :80              │        │ 2ª validación JWT · :8080 │   │
                  │  └──────────────────┘        └──────┬──────────┬─────────┘   │
                  │                    red interna Docker│          │             │
                  │  ┌─────────────┐  ┌──────────────┐  ┌▼──────────▼──┐          │
                  │  │ ms-catalogo │◄─│ ms-usuarios  │◄─│ ms-citas     │          │
                  │  │ :8083       │  │ :8084        │  │ :8082        │          │
                  │  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘          │
                  └─────────┼────────────────┼─────────────────┼──────────────────┘
                            └──────── TLS :1521 ───────────────┘
                                             ▼
                      Oracle Autonomous Database (Oracle Cloud)
                      esquemas CATALOGO · USUARIOS · CITAS
```

| Componente | Dónde corre | Contenedor / puerto | ¿Accesible desde Internet? |
|---|---|---|---|
| Frontend React | EC2 | `frontend` (nginx) · 80 | Solo a través de API Gateway (`GET /…`) |
| BFF | EC2 | `bff` · 8080 | Solo a través de API Gateway (`/api/…`, con JWT) |
| ms-citas | EC2 | `ms-citas` · 8082 | **No** (solo la red interna de Docker) |
| ms-usuarios | EC2 | `ms-usuarios` · 8084 | **No** |
| ms-catalogo | EC2 | `ms-catalogo` · 8083 | **No** |
| Base de datos | Oracle Cloud | Autonomous DB · 1521 (TLS) | Solo desde las IP autorizadas (EC2 y tu Mac) |
| Identidad | Microsoft | Entra ID | Login de Microsoft |

**Por qué así:**
- **Entra ID exige HTTPS** en la dirección de retorno del login (salvo `localhost`). API Gateway entrega HTTPS con su propia URL, sin comprar dominio ni configurar certificados.
- **Un solo punto de entrada:** el usuario abre la URL de API Gateway y desde ahí carga la aplicación y llama a la API. Como el frontend y la API comparten dominio, el navegador no necesita CORS; igual se deja configurado para desarrollo y porque lo pide el alcance.
- **Una sola EC2 con Docker Compose:** cada servicio es un contenedor independiente (imagen, proceso y puerto propios), pero se levantan todos con un solo comando. Es lo más simple de operar en AWS Academy. Si el profesor exige una instancia por servicio, está en el anexo 4.10.

**Flujo de una acción (por ejemplo, un paciente reserva una hora):**

1. El navegador abre `https://…amazonaws.com/`. API Gateway reenvía `GET /` a nginx (EC2:80), que entrega la app React.
2. La app redirige al login de Microsoft. Entra ID devuelve el access token a `https://…amazonaws.com/`.
3. La app llama `POST https://…amazonaws.com/api/citas` con `Authorization: Bearer <token>` (MsalInterceptor).
4. API Gateway valida el JWT (firma, `iss`, `aud`, `exp`, `scp`). Si no es válido responde 401 y la solicitud no llega a la EC2.
5. API Gateway reenvía la solicitud al BFF (EC2:8080), que **vuelve a validar el JWT** y comprueba que el rol sea Paciente.
6. El BFF llama a ms-citas (`http://ms-citas:8082`, red interna) con la identidad del usuario.
7. ms-citas consulta a ms-usuarios que el paciente y el médico estén habilitados, y guarda la cita en el esquema `CITAS` de Oracle.
8. La respuesta vuelve por el mismo camino hasta el navegador.

---

## 4.2 Antes de empezar (checklist)

- [ ] Entra ID configurado: usuarios, `gestorcitas-api` con roles y scope, y `gestorcitas-frontend` (Fase 1 de la guía).
- [ ] Oracle Autonomous DB creada, con los 3 esquemas (`docs/03-oracle-autonomous-db.md`).
- [ ] La aplicación funciona **en local** con `docker compose up -d --build` en http://localhost (Fase 2).
- [ ] El código está en GitHub.
- [ ] Tienes tu `.env` de la raíz funcionando en local (lo vas a copiar a la EC2).

---

## 4.3 Paso 1 · Crear la EC2

**Key pair:** EC2 → *Key Pairs* → **Create key pair** → nombre `gestorcitas-key`, tipo RSA, formato `.pem`.

```bash
mv ~/Downloads/gestorcitas-key.pem ~/.ssh/ && chmod 400 ~/.ssh/gestorcitas-key.pem
```

**Security Group:** EC2 → *Security Groups* → **Create** → nombre `gestorcitas-sg`:

| Tipo | Puerto | Origen | Para qué |
|---|---|---|---|
| SSH | 22 | **My IP** | Administrar la instancia |
| HTTP | 80 | `0.0.0.0/0` | API Gateway → frontend |
| Custom TCP | 8080 | `0.0.0.0/0` | API Gateway → BFF |

> API Gateway no tiene IP fija, por eso 80 y 8080 quedan abiertos. El frontend es contenido público, y el BFF rechaza con 401 cualquier llamada sin un JWT válido de Entra ID. Los microservicios (8082–8084) **no** se abren.

**Instancia:** EC2 → **Launch instance**

| Campo | Valor |
|---|---|
| Name | `gestorcitas` |
| AMI | **Amazon Linux 2023** (64-bit x86) |
| Instance type | **t3.medium** (4 GB; con t3.small pon `JAVA_XMX=256m`) |
| Key pair | `gestorcitas-key` |
| Security group | `gestorcitas-sg` |
| Storage | **20 GiB** gp3 |
| Advanced → User data | contenido de `infra/aws/ec2-user-data.sh` (instala Docker, Docker Compose y git) |

**Launch instance** y espera a que el estado sea *Running* y los *Status checks* digan *2/2 checks passed*.

## 4.4 Paso 2 · IP fija y permiso en Oracle

1. EC2 → **Elastic IPs** → **Allocate Elastic IP address** → **Allocate**.
2. Selecciónala → **Actions → Associate Elastic IP address** → instancia `gestorcitas` → **Associate**.
3. Anota:
   - `EC2_IP`, por ejemplo `3.90.12.34`
   - `EC2_HOST`, el *Public IPv4 DNS*, por ejemplo `ec2-3-90-12-34.compute-1.amazonaws.com`
4. **Oracle Cloud** → tu Autonomous Database → **Network → Access control list → Edit** → agrega `EC2_IP` → **Save**.

> La Elastic IP no cambia aunque se reinicie la instancia o el Learner Lab. Sin ella tendrías que actualizar API Gateway y Oracle cada vez.

## 4.5 Paso 3 · Llevar el código y la configuración a la EC2

```bash
ssh -i ~/.ssh/gestorcitas-key.pem ec2-user@<EC2_IP>
docker --version && docker compose version && git --version   # lo instaló el user data
```

Si `docker` responde *permission denied*, sal (`exit`) y vuelve a entrar.

**Clonar el repositorio.** Como es privado, GitHub pide un token:

1. En GitHub → *Settings* → *Developer settings* → *Personal access tokens* → **Fine-grained tokens** → **Generate new token**.
2. *Repository access*: solo `GestorCitaCloudNative`. *Permissions → Contents*: **Read-only**.
3. En la EC2:

```bash
git clone https://<tu-usuario>:<TOKEN>@github.com/<tu-usuario>/GestorCitaCloudNative.git
cd GestorCitaCloudNative
```

**Copiar el `.env`.** Desde otra terminal en tu Mac, así no tienes que escribirlo a mano:

```bash
scp -i ~/.ssh/gestorcitas-key.pem ~/IdeaProjects/GestorCitaCloudNative/.env \
    ec2-user@<EC2_IP>:~/GestorCitaCloudNative/.env
```

El `.env` contiene la conexión a Oracle, las contraseñas de los esquemas y los IDs de Entra ID. No está en GitHub, por eso se copia aparte.

## 4.6 Paso 4 · Levantar los 5 contenedores

En la EC2:

```bash
cd ~/GestorCitaCloudNative
docker compose up -d --build          # la primera vez: 8–12 min (compila Java y React)
docker compose ps                     # 5 servicios en estado "running"
```

Verificación **dentro de la EC2**:

```bash
curl -s  http://localhost/ | head -5                   # HTML de la app (frontend OK)
curl -s  http://localhost:8080/actuator/health         # {"status":"UP"}  (BFF OK)
curl -si http://localhost:8080/api/citas/mias | head -1   # HTTP/1.1 401    (BFF exige token)
docker compose logs ms-usuarios | grep -i "started\|médicos"   # conectó a Oracle y cargó datos
```

Si un microservicio queda reiniciándose, revisa `docker compose logs <servicio>`. Lo más común es que falte la IP de la EC2 en la lista de acceso de Oracle (paso 2).

## 4.7 Paso 5 · Crear API Gateway

### Opción A — Script (recomendado)

En tu **Mac**, con la AWS CLI. En Learner Lab: *AWS Details → AWS CLI → Show* y copia el bloque en `~/.aws/credentials`.

```bash
cd ~/IdeaProjects/GestorCitaCloudNative
export AWS_REGION=us-east-1
export ENTRA_TENANT_ID=<TENANT_ID>
export ENTRA_API_CLIENT_ID=<API_CLIENT_ID>
export EC2_HOST=<EC2_HOST>
./infra/aws/api-gateway.sh
```

Al terminar muestra la URL de la aplicación (`https://abc123.execute-api.us-east-1.amazonaws.com`) y los dos pasos que faltan.

### Opción B — Consola

1. **API Gateway → Create API → HTTP API → Build** → nombre `gestorcitas-http-api` → *Next* → no agregues rutas → *Next* → stage `$default` con **Auto-deploy** → **Create**.
2. **Integrations → Manage integrations → Create**, 3 veces (tipo **HTTP URI**):

   | Integración | Método | URL |
   |---|---|---|
   | bff | ANY | `http://<EC2_HOST>:8080/api/{proxy}` |
   | frontend-root | GET | `http://<EC2_HOST>:80/` |
   | frontend | GET | `http://<EC2_HOST>:80/{proxy}` |

3. **Routes → Create**, 4 veces, y en cada ruta **Attach integration**:

   | Ruta | Integración | Autorización |
   |---|---|---|
   | `ANY /api/{proxy+}` | bff | JWT (paso 4) |
   | `OPTIONS /api/{proxy+}` | bff | ninguna |
   | `GET /` | frontend-root | ninguna |
   | `GET /{proxy+}` | frontend | ninguna |

   > API Gateway siempre elige la ruta más específica: `GET /api/citas` va a `/api/{proxy+}` (BFF), y `GET /paciente/reservar` va a `/{proxy+}` (frontend).

4. **Authorization** → selecciona `ANY /api/{proxy+}` → **Create and attach an authorizer**:
   - Type **JWT** · Name `entra-id-jwt` · Identity source `$request.header.Authorization`
   - Issuer `https://login.microsoftonline.com/<TENANT_ID>/v2.0`
   - Audience `<API_CLIENT_ID>`
   - Guarda. En la ruta, *Authorization scopes* → `access_as_user`.
5. **CORS → Configure**:
   - Allow origins: `https://<tu-api>.execute-api.us-east-1.amazonaws.com`, `http://localhost:5173`, `http://localhost`
   - Allow headers: `authorization, content-type`
   - Allow methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
   - Max age `3600`
6. Copia la **Invoke URL** del stage `$default`: esa es la URL de la aplicación.

## 4.8 Paso 6 · Conectar Entra ID y el BFF con la URL pública

1. **Entra ID → App registrations → gestorcitas-frontend → Authentication → Single-page application → Add URI**:
   - `https://abc123.execute-api.us-east-1.amazonaws.com`
   - `https://abc123.execute-api.us-east-1.amazonaws.com/login`
   - **Save**
2. **En la EC2**, en `.env`:

   ```env
   CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost,https://abc123.execute-api.us-east-1.amazonaws.com
   ```

   y aplica con `docker compose up -d bff`.

No hay que recompilar el frontend: toma la URL de la API y la del login del dominio desde donde se abre.

## 4.9 Paso 7 · Probar (evidencias para la evaluación)

Abre `https://abc123.execute-api.us-east-1.amazonaws.com` e inicia sesión con cada integrante:

| Evidencia | Cómo mostrarla |
|---|---|
| Login y logout con MSAL | Botón "Iniciar sesión con Microsoft" y "Cerrar sesión" |
| Guards | Sin sesión, `…/admin` redirige a `/login`; Cesar (Paciente) en `…/admin` ve "Acceso no autorizado" |
| MsalInterceptor | DevTools → Network → cualquier `/api/…` lleva `Authorization: Bearer …` |
| 1ª validación (API Gateway) | `curl -i https://…amazonaws.com/api/citas/mias` → **401** `{"message":"Unauthorized"}` |
| CORS y OPTIONS | `curl -i -X OPTIONS https://…/api/citas/mias -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET"` → 204 con cabeceras `access-control-allow-*` |
| 2ª validación (BFF) | `curl -i http://<EC2_IP>:8080/api/citas/mias` → **401** del BFF; token de Cesar contra `GET /api/usuarios` → **403** |
| Pantallas por actor | Cesar: reservar y mis citas · Elian: agenda y atender · Aaron: panel, usuarios, especialidades y citas |
| Persistencia en Oracle | En Oracle Database Actions, ejecuta `infra/db/oracle/adb/02-verificar-datos.sql` |
| Microservicios no expuestos | `curl --max-time 5 http://<EC2_IP>:8082` → sin respuesta (timeout) |

---

## Operación diaria

| Tarea | Comando (en la EC2, carpeta del proyecto) |
|---|---|
| Ver estado | `docker compose ps` |
| Ver logs de un servicio | `docker compose logs -f bff` |
| Actualizar tras un cambio en GitHub | `git pull && docker compose up -d --build` |
| Actualizar solo un servicio | `git pull && docker compose up -d --build ms-citas` |
| Reiniciar todo | `docker compose restart` |
| Apagar | `docker compose down` |

**AWS Academy Learner Lab:** al terminar la sesión la EC2 se apaga. Al volver: *Start Lab* → la EC2 enciende sola → los contenedores arrancan solos (`restart: unless-stopped`). La Elastic IP y API Gateway siguen iguales. Si la Autonomous DB lleva 7 días sin uso, enciéndela en Oracle Cloud con **Start**.

---

## 4.10 Anexo · Variante "una EC2 por servicio"

Solo si la pauta lo exige. Se usa el mismo repositorio y el mismo `docker-compose.yml`; cambian el `.env` y el comando de cada instancia.

| Instancia | Comando | Variables extra en su `.env` | Puertos en su Security Group |
|---|---|---|---|
| `gc-frontend` | `docker compose up -d --build --no-deps frontend` | — | 80 desde 0.0.0.0/0 |
| `gc-bff` | `docker compose up -d --build --no-deps bff` | `CITAS_URL=http://<IP privada ms-citas>:8082`<br>`USUARIOS_URL=http://<IP privada ms-usuarios>:8084`<br>`CATALOGO_URL=http://<IP privada ms-catalogo>:8083` | 8080 desde 0.0.0.0/0 |
| `gc-ms-citas` | `docker compose -f docker-compose.yml -f infra/aws/docker-compose.multi-ec2.yml up -d --build --no-deps ms-citas` | `USUARIOS_URL=http://<IP privada ms-usuarios>:8084` | 8082 solo desde el SG del BFF |
| `gc-ms-usuarios` | igual, con `ms-usuarios` | `CATALOGO_URL=http://<IP privada ms-catalogo>:8083` | 8084 desde los SG del BFF y de ms-citas |
| `gc-ms-catalogo` | igual, con `ms-catalogo` | — | 8083 desde los SG del BFF y de ms-usuarios |

- API Gateway: la integración del frontend apunta a `gc-frontend` y la del BFF a `gc-bff`.
- Las IP de las tres instancias de microservicios deben estar en la lista de acceso de Oracle.
- Con 5 instancias t3.small, usa `JAVA_XMX=256m`.
