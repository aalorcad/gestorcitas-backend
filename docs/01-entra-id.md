# 1. Configuración de Microsoft Entra ID

Se crean **dos App Registrations**: una para la API (recurso protegido, define scope y roles) y otra para el frontend React (cliente SPA que pide tokens).

## 1.1 Tenant y usuarios de prueba

1. Portal Azure → **Microsoft Entra ID** → *Overview*. Anota el **Tenant ID** (Directory ID) y el dominio (`xxxx.onmicrosoft.com`).
2. *Users* → **New user → Create new user**. Crea:

| Usuario | UPN | Rol que se asignará |
|---|---|---|
| Paciente Uno | `paciente1@<tenant>.onmicrosoft.com` | Paciente |
| Médico Uno | `medico1@<tenant>.onmicrosoft.com` | Medico |
| Admin Uno | `admin1@<tenant>.onmicrosoft.com` | Admin |

> El email de `medico1` debe ir en `DEMO_MEDICO_EMAIL` del `.env`: ms-usuarios lo pre-registra como "Dr. Elian Barra" y lo vincula con la cuenta de Entra ID en su primer login. Para nuevos médicos, el Admin los registra desde **Usuarios → Registrar médico** con el mismo UPN que tienen en Entra ID.
>
> Pacientes y administradores no necesitan pre-registro: se crean automáticamente en ms-usuarios en su primer inicio de sesión.

## 1.2 App Registration de la API — `gestorcitas-api`

1. *App registrations* → **New registration**
   - Name: `gestorcitas-api`
   - Supported account types: *Accounts in this organizational directory only*
   - Sin Redirect URI → **Register**
2. Anota el **Application (client) ID** → `ENTRA_API_CLIENT_ID`.
3. **Expose an API**
   - *Application ID URI* → **Add** → **deja el valor sugerido** `api://<client-id>` → `ENTRA_API_APP_ID_URI`. Los tenants nuevos rechazan URIs personalizadas como `api://gestorcitas-api` (*identifier-uri-formatting-error*).
   - **Add a scope**: name `access_as_user`, *Who can consent*: Admins and users, display name "Acceder a Gestor de Citas como el usuario" → *Enabled*.
4. **App roles** → *Create app role* (3 veces):

| Display name | Allowed member types | Value | Description |
|---|---|---|---|
| Paciente | Users/Groups | `Paciente` | Reserva y gestiona sus citas |
| Medico | Users/Groups | `Medico` | Gestiona su agenda |
| Admin | Users/Groups | `Admin` | Administra catálogo y citas |

   > El *Value* debe ser exactamente `Paciente`, `Medico`, `Admin` (el BFF y el frontend lo comparan así).
5. **Manifest** → establece tokens v2:
   - Editor nuevo: `"api": { "requestedAccessTokenVersion": 2 }`
   - Editor antiguo: `"accessTokenAcceptedVersion": 2`

   Con v2 el token trae `iss = https://login.microsoftonline.com/<tenant>/v2.0` y `aud = <client-id de la API>`, que es lo que validan API Gateway y el BFF.

## 1.3 App Registration del frontend — `gestorcitas-frontend`

1. **New registration**
   - Name: `gestorcitas-frontend`
   - Redirect URI: plataforma **Single-page application (SPA)** → `http://localhost:5173`
2. Anota el **Application (client) ID** → `VITE_ENTRA_CLIENT_ID`.
3. *Authentication* → agrega también:
   - `http://localhost:5173/login` (post-logout)
   - `http://localhost` y `http://localhost/login` (la app completa en Docker).
   - En AWS: la URL de API Gateway y la misma con `/login` (ver `04-despliegue-ec2.md`, paso 6).
4. **API permissions** → *Add a permission* → *My APIs* → `gestorcitas-api` → *Delegated* → `access_as_user` → **Add**.
5. **Grant admin consent for <tenant>**.

## 1.4 Asignación de roles a usuarios

1. *Enterprise applications* → `gestorcitas-api` → **Users and groups** → *Add user/group*.
2. Asigna `paciente1` → Paciente, `medico1` → Medico, `admin1` → Admin (un usuario puede tener varios roles).
3. (Opcional) *Properties* → **Assignment required? = Yes** para que solo usuarios asignados puedan obtener tokens.

Los roles viajan en el claim `roles` del **access token de la API**. El frontend lo lee para mostrar menús y proteger rutas; API Gateway y el BFF lo vuelven a validar.

## 1.5 Resumen de valores

| Valor | Dónde se obtiene | Dónde se usa |
|---|---|---|
| Tenant ID | Entra ID → Overview | `VITE_ENTRA_TENANT_ID`, `ENTRA_TENANT_ID`, issuer del API Gateway |
| Client ID frontend | App reg. `gestorcitas-frontend` | `VITE_ENTRA_CLIENT_ID` |
| Client ID API | App reg. `gestorcitas-api` | `ENTRA_API_CLIENT_ID`, audience del API Gateway |
| Application ID URI | Expose an API | `ENTRA_API_APP_ID_URI` |
| Scope | `api://<client-id-api>/access_as_user` | `VITE_API_SCOPE` / `API_SCOPE` |
| Authority | `https://login.microsoftonline.com/<tenant-id>` | `msalConfig.ts` (se arma desde el Tenant ID) |
| Redirect URI | `http://localhost:5173` | `VITE_ENTRA_REDIRECT_URI` |

## 1.6 Verificar el token

Inicia sesión en el frontend y abre **Perfil**: muestra los claims que el BFF leyó tras validar el token (`aud`, `iss`, `roles`, `scp`). También puedes copiar el token desde DevTools → Network → header `Authorization` y pegarlo en <https://jwt.ms>.
