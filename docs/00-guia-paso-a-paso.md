# Guía paso a paso: de código a funcionando (local y AWS)

Orden recomendado. **No saltes fases**: cada una usa valores de la anterior.

| Fase | Qué haces | Tiempo aprox. |
|---|---|---|
| 0 | Instalar herramientas | 15 min |
| 1 | Configurar Microsoft Entra ID | 30 min |
| 1B | Crear Oracle Autonomous Database (1 instancia, 3 esquemas) | 20 min |
| 2 | Levantar todo **en local** (los 5 contenedores) y probar los 3 roles | 20 min |
| 3 | Desplegar en AWS: EC2 (frontend + BFF + microservicios) y API Gateway | 60 min |
| 4 | Verificación final | 10 min |

> Ve anotando los valores en la **hoja de valores** del final.

---

## Fase 0 · Herramientas en tu Mac

```bash
java -version        # 17 o superior
node -v              # 20 o superior
docker --version     # Docker Desktop instalado y ABIERTO
```

- Docker Desktop: https://www.docker.com/products/docker-desktop/ (elige Apple Silicon o Intel según tu Mac).
- Node: `brew install node@20` o desde nodejs.org.
- AWS CLI (solo si usarás el script de API Gateway): `brew install awscli`.

Compila una vez para confirmar que el código está sano:

```bash
cd ~/IdeaProjects/GestorCitaCloudNative
./mvnw clean test
```

---

## Fase 1 · Microsoft Entra ID

### 1.1 Consigue un tenant donde seas administrador

⚠️ **No uses el tenant de Duoc** (`duocuc.cl`): como alumno no puedes crear App Registrations ni asignar roles ahí.

1. Entra a https://portal.azure.com con una cuenta personal (Outlook/Hotmail) o activa **Azure for Students** (https://azure.microsoft.com/free/students) con tu correo Duoc.
2. Al crear la cuenta se genera un tenant propio ("Default Directory") donde eres **Global Administrator**.
3. Portal → buscar **Microsoft Entra ID** → *Overview*. Anota:
   - **Tenant ID** → hoja de valores `TENANT_ID`
   - **Primary domain** (ej. `aaronlorcaoutlook.onmicrosoft.com`) → `DOMINIO`

> Entra ID Free alcanza para todo el proyecto (usuarios, App Registrations y asignación de roles a usuarios).

### 1.2 Crea los usuarios de prueba

Entra ID → **Users** → **New user** → **Create new user**. Crea estos 3 (marca *Auto-generate password* o define una y anótala):

| Display name | User principal name | Rol que tendrá |
|---|---|---|
| Paciente Uno | `paciente1@DOMINIO` | Paciente |
| Médico Uno | `medico1@DOMINIO` | Medico |
| Admin Uno | `admin1@DOMINIO` | Admin |

> En el primer login cada usuario debe cambiar su contraseña. Te conviene iniciar sesión una vez con cada uno en https://myapps.microsoft.com (usa una ventana de incógnito) para dejar lista la contraseña definitiva.

### 1.3 App Registration de la API: `gestorcitas-api`

1. Entra ID → **App registrations** → **New registration**
   - Name: `gestorcitas-api`
   - Supported account types: **Accounts in this organizational directory only (Single tenant)**
   - Redirect URI: vacío → **Register**
2. En *Overview* anota el **Application (client) ID** → `API_CLIENT_ID`.
3. **Expose an API**
   1. *Application ID URI* → **Add** → **deja el valor sugerido** `api://<API_CLIENT_ID>` → **Save** → `API_APP_ID_URI`.
      (Los tenants nuevos rechazan URIs inventadas como `api://gestorcitas-api` con el error *identifier-uri-formatting-error*.)
   2. **Add a scope**:
      - Scope name: `access_as_user`
      - Who can consent: **Admins and users**
      - Admin consent display name: `Acceder a Gestor de Citas`
      - Admin consent description: `Permite a la app llamar a la API en nombre del usuario`
      - State: **Enabled** → **Add scope**
4. **App roles** → **Create app role**, 3 veces:

   | Display name | Allowed member types | Value | Description |
   |---|---|---|---|
   | Paciente | Users/Groups | `Paciente` | Reserva y gestiona sus citas |
   | Medico | Users/Groups | `Medico` | Gestiona su agenda |
   | Admin | Users/Groups | `Admin` | Administra usuarios, catálogo y citas |

   ⚠️ El **Value** tiene que ser exactamente `Paciente`, `Medico` y `Admin` (con esas mayúsculas y sin tilde).

5. **Manifest** (menú izquierdo) → busca y cambia:
   - Editor nuevo (*Microsoft Graph App Manifest*): `"requestedAccessTokenVersion": null` → **`2`** (dentro de `"api": { ... }`).
   - Editor antiguo (*AAD Graph*): `"accessTokenAcceptedVersion": null` → **`2`**.
   - **Save**.

   ⚠️ **Este es el paso que más se olvida.** Sin él, el token trae `iss=https://sts.windows.net/...` y tanto API Gateway como el BFF lo rechazan con 401.

6. **Token configuration** → **Add optional claim** → Token type **Access** → marca `email` y `upn` → **Add** (si pregunta por permisos de Graph, acepta).
   Así el backend recibe siempre el email del usuario, que ms-usuarios usa para vincular a los médicos.

### 1.4 App Registration del frontend: `gestorcitas-frontend`

1. **New registration**
   - Name: `gestorcitas-frontend`
   - Account types: **Single tenant**
   - Redirect URI: plataforma **Single-page application (SPA)** → `http://localhost:5173` → **Register**
2. Anota el **Application (client) ID** → `FRONT_CLIENT_ID`.
3. **Authentication** → en la plataforma SPA agrega también estas URIs → **Save**:
   - `http://localhost:5173/login` (desarrollo con `npm run dev`)
   - `http://localhost` y `http://localhost/login` (la app completa con Docker en tu Mac)
   - Las URIs de AWS (`https://…execute-api…amazonaws.com`) se agregan en la Fase 3, cuando exista API Gateway.
   (Asegúrate de que diga *Single-page application* y **no** *Web*; si dice Web, MSAL falla con AADSTS9002326).
4. **API permissions** → **Add a permission** → **My APIs** → `gestorcitas-api` → *Delegated permissions* → marca `access_as_user` → **Add permissions**.
5. Clic en **Grant admin consent for Default Directory** → **Yes**. La columna Status debe quedar en verde.

### 1.5 Asigna los roles a los usuarios

1. Entra ID → **Enterprise applications** → quita el filtro *Application type* si no aparece → abre **gestorcitas-api**.
2. **Users and groups** → **Add user/group**:
   - Users: `paciente1` → Select a role: **Paciente** → **Assign**
   - Repite: `medico1` → **Medico**, `admin1` → **Admin**
3. (Recomendado) **Properties** → *Assignment required?* → **Yes** → Save. Así solo los usuarios asignados pueden obtener tokens.

> Si quieres probar todo con una sola cuenta, asígnale los 3 roles a `admin1`: verá los 3 portales en el menú.

### 1.6 Comprobar el token (opcional pero recomendado)

Lo harás en la Fase 2: tras iniciar sesión, abre **Sesión y token** en la app. Tienes que ver:
- `iss` = `https://login.microsoftonline.com/<TENANT_ID>/v2.0`
- `aud` = `<API_CLIENT_ID>`
- `roles` con tu rol y `scp` = `access_as_user`

---

## Fase 1B · Oracle Autonomous Database

Detalle completo, con cada campo de la consola: **[03-oracle-autonomous-db.md](03-oracle-autonomous-db.md)**. Resumen:

1. Crea una cuenta **Oracle Cloud Free Tier**, con Home region Santiago o São Paulo.
2. **Create Autonomous Database**: Transaction Processing, Serverless, **Always Free**, ADMIN password, acceso *Secure access from allowed IPs* con **tu IP**, y *Require mTLS* **desmarcado**.
3. **Database connection → TLS** → copia la cadena de `gestorcitas_low`. Anota `jdbc:oracle:thin:@` + cadena → `DB_URL`.
4. **Database actions → SQL** (como ADMIN) → pega `infra/db/oracle/adb/01-crear-esquemas.sql` → **Run Script (F5)**. Se crean los esquemas `CATALOGO`, `USUARIOS` y `CITAS`.

> Las tablas no se crean a mano: cada microservicio crea las suyas en su esquema la primera vez que arranca.
>
> ¿Aún no tienes la base en Oracle Cloud? Puedes avanzar en local con Oracle Free en Docker (sección 3.7 de ese documento) y conectar la Autonomous DB después.

---

## Fase 2 · Levantar en local

En tu Mac corre **exactamente lo mismo que en la EC2**: los 5 contenedores con el mismo `docker-compose.yml`.

### 2.1 Variables (un solo `.env` en la raíz)

```bash
cd ~/IdeaProjects/GestorCitaCloudNative
cp .env.example .env
```

Edita `.env`:

```env
# Oracle Autonomous Database (Fase 1B)
DB_URL='jdbc:oracle:thin:@(description=...gestorcitas_low.adb.oraclecloud.com...)'
CATALOGO_DB_USER=CATALOGO
CATALOGO_DB_PASSWORD=<CLAVE_CATALOGO>
USUARIOS_DB_USER=USUARIOS
USUARIOS_DB_PASSWORD=<CLAVE_USUARIOS>
CITAS_DB_USER=CITAS
CITAS_DB_PASSWORD=<CLAVE_CITAS>
DB_POOL_SIZE=4

# Entra ID (Fase 1)
ENTRA_TENANT_ID=<TENANT_ID>
ENTRA_API_CLIENT_ID=<API_CLIENT_ID>
ENTRA_API_APP_ID_URI=api://<API_CLIENT_ID>
ENTRA_REQUIRED_SCOPE=access_as_user
FRONT_CLIENT_ID=<FRONT_CLIENT_ID>
API_SCOPE=api://<API_CLIENT_ID>/access_as_user
DEMO_MEDICO_EMAIL=medico1@<DOMINIO>
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost
JAVA_XMX=384m
```

### 2.2 Levantar todo con Docker

```bash
docker compose up -d --build        # la primera vez tarda 8–12 min (compila Java y React)
docker compose ps                   # 5 contenedores "running": frontend, bff, ms-citas, ms-usuarios, ms-catalogo
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

Abre **http://localhost**: es la aplicación completa (nginx sirve React y reenvía `/api` al BFF).

Si algo falla: `docker compose logs -f bff` (o `frontend`, `ms-usuarios`, `ms-citas`, `ms-catalogo`).

Comprueba la persistencia en Oracle: en **Database actions → SQL** ejecuta `infra/db/oracle/adb/02-verificar-datos.sql`. Deben aparecer las tablas de cada esquema, las 4 especialidades y los 3 médicos.

> Si un microservicio queda reiniciándose con `ORA-12506` o `Connection reset`, tu IP no está en la lista de acceso de la Autonomous DB (sección 3.6 del documento de Oracle).

### 2.3 (Opcional) Frontend en modo desarrollo

Solo si vas a modificar pantallas y quieres ver los cambios al instante. El backend sigue en Docker.

```bash
cd frontend
cp .env.example .env      # completa VITE_ENTRA_CLIENT_ID y VITE_ENTRA_TENANT_ID
npm install
npm run dev               # http://localhost:5173 (usa la API en http://localhost:8080)
```

### 2.4 Prueba los 3 roles (usa ventana de incógnito para cambiar de usuario)

**admin1**
1. Entras al **Panel** con los indicadores.
2. **Usuarios**: ves a los 3 médicos pre-registrados ("Pendiente de primer inicio de sesión").
3. **Especialidades**: crea una, edítala y cambia su valor de consulta.

**medico1**
1. Al entrar queda vinculado a "Dr. Elian Barra" (Medicina General).
2. **Mi perfil**: agrega un teléfono y tu presentación.

**paciente1**
1. Aparece el aviso "Completa tu perfil". En **Mi perfil** ingresa un RUT válido (ej. `11.111.111-1`), tu fecha de nacimiento y tu previsión.
2. **Reservar**: Medicina General → Dr. Elian Barra → un día hábil → un bloque → **Reservar**.
3. **Mis citas**: la cita aparece en estado PENDIENTE.

**medico1 de nuevo**
- **Mi agenda** → Confirmar → Atender (agrega observaciones).

**admin1 de nuevo**
- **Citas**: aparece la cita ATENDIDA. En **Usuarios**, prueba desactivar a paciente1: cuando ese usuario vuelva a entrar, verá "Cuenta desactivada".

✅ Si todo esto funciona, el código y Entra ID están listos. Recién entonces pasa a AWS.

---

## Fase 3 · Despliegue en AWS

📘 **Paso a paso completo, con diagrama y cada campo de la consola: [04-despliegue-ec2.md](04-despliegue-ec2.md)**

Resumen de lo que queda en AWS (el frontend sigue local):

```
Frontend local ──HTTPS──► API Gateway: 25 rutas /api/... + JWT (1ª val.) ─► EC2-A :8080 bff (2ª validación)
                                                                             └─► EC2-B :8082-8084 microservicios
                                                                                  (IP privada) ──► Oracle ADB
```

1. **2 EC2** t3.medium Amazon Linux 2023 con `infra/aws/ec2-user-data.sh`: `gestorcitas-bff` (SG: 22 tu IP, 8080) y `gestorcitas-ms` (SG: 22 tu IP, 8082-8084 solo desde el SG del BFF).
2. **IP elástica** para cada una; la de EC2-B va en la lista de acceso de la Autonomous DB.
3. En cada EC2: `git clone` de `gestorcitas-backend` y `scp` de tu `.env` (en EC2-A con `MS_HOST` = IP privada de EC2-B).
4. EC2-B: `docker compose -f docker-compose.ec2-ms.yml up -d --build`. EC2-A: `docker compose -f docker-compose.ec2-bff.yml up -d --build`.
5. **API Gateway** con `infra/aws/api-gateway.sh` desde CloudShell: autorizador JWT, 25 rutas, OPTIONS y CORS.
6. En `frontend/.env`: `VITE_API_BASE_URL=<URL de API Gateway>` y `npm run dev`.

---

## Fase 4 · Verificación final (checklist para la presentación)

- [ ] Login y logout con Microsoft (MSAL).
- [ ] Sin sesión, `/admin` redirige a `/login` (AuthGuard).
- [ ] paciente1 que intenta abrir `/admin` va a "Acceso no autorizado" (RoleGuard).
- [ ] DevTools → Network → toda llamada a `execute-api` lleva `Authorization: Bearer …` (MsalInterceptor).
- [ ] `curl` sin token a API Gateway → 401 (1ª validación).
- [ ] `curl` directo al BFF en `:8080` sin token → 401 (2ª validación).
- [ ] Token de paciente contra `GET /api/usuarios` → 403 del BFF (rol).
- [ ] Preflight OPTIONS → 204 con cabeceras CORS.
- [ ] Paciente: completar perfil → reservar → cancelar.
- [ ] Médico: ver agenda → confirmar → atender.
- [ ] Admin: panel, registrar médico, desactivar usuario, especialidades y citas.
- [ ] `docker ps` en EC2-A (bff) y EC2-B (3 microservicios); puertos 8082–8084 no accesibles desde Internet.
- [ ] Postman: colección `infra/postman` en verde (401 / 200 / 403 / 404).
- [ ] Oracle Autonomous DB: `02-verificar-datos.sql` muestra los datos en los esquemas `CATALOGO`, `USUARIOS` y `CITAS`.

---

## Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `AADSTS50011` redirect URI mismatch | La URI no está registrada o es de tipo Web | Registra la URI exacta como **SPA** en `gestorcitas-frontend` |
| `AADSTS9002326` cross-origin token redemption | La plataforma quedó como *Web* | Borra la URI de Web y agrégala en **Single-page application** |
| `AADSTS65001` consent required | Falta el consentimiento | *API permissions* → **Grant admin consent** |
| `AADSTS50105` user not assigned | *Assignment required* = Yes y el usuario no tiene rol | Asígnale un rol en *Enterprise applications* |
| 401 de API Gateway con token | El token es v1 (iss `sts.windows.net`) | Manifest de `gestorcitas-api` → `requestedAccessTokenVersion: 2`, luego cierra sesión y vuelve a entrar |
| 401 de API Gateway "insufficient scope" | El authorizer no reconoce el scope | Quita *Authorization scopes* de la ruta (el BFF igual valida `scp`) |
| 401 del BFF | `ENTRA_TENANT_ID`/`ENTRA_API_CLIENT_ID` mal copiados en `.env` | Corrígelos y `docker compose up -d bff` |
| "Tu cuenta no tiene un rol asignado" | Roles vacíos en el token | Asigna el rol en *Enterprise applications → gestorcitas-api*, cierra sesión y entra de nuevo |
| El médico no ve su agenda ni su perfil | El email del usuario Entra ≠ email pre-registrado | Usa `DEMO_MEDICO_EMAIL` = UPN exacto, o regístralo desde Admin → Usuarios |
| Error CORS en el navegador | Origen no permitido | Agrega el origen en API Gateway (CORS) y en `CORS_ALLOWED_ORIGINS` |
| 503 "ms-usuarios no disponible" | Un contenedor cayó | `docker compose ps` / `docker compose logs ms-usuarios` |
| 500/504 de API Gateway | La EC2 cambió de IP o está apagada | Usa una Elastic IP; revisa la URL de la integración |
| Contenedores se reinician (`Exited 137`) | Falta memoria | t3.medium, o `JAVA_XMX=256m` en `.env` |
| `ORA-12506` / `Connection reset` en los logs | La IP (tu Mac o la EC2) no está en la lista de acceso de la Autonomous DB | OCI → Network → Access control list → agrega la IP |
| `ORA-01017 invalid username/password` | La contraseña del esquema no coincide con el `.env` | Revisa `*_DB_PASSWORD` o cambia la clave con `ALTER USER` |
| Los microservicios no conectan a Oracle tras varios días | La Always Free se detuvo por inactividad | OCI → tu base → **Start** |

---

## Hoja de valores

| Clave | Valor |
|---|---|
| `TENANT_ID` | |
| `DOMINIO` | |
| `API_CLIENT_ID` | |
| `API_APP_ID_URI` | `api://<API_CLIENT_ID>` |
| `FRONT_CLIENT_ID` | |
| Scope | `api://<API_CLIENT_ID>/access_as_user` |
| Authority | `https://login.microsoftonline.com/<TENANT_ID>` |
| Issuer (API Gateway) | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `DB_URL` (TLS `_low`) | |
| ADMIN password de la ADB (no la subas a GitHub) | |
| `EC2_IP` | |
| `EC2_DNS` | |
| `API_GW_URL` | |
| URL de la aplicación (API Gateway `$default`) | |
