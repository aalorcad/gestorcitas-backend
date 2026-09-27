# 1B. Microsoft Entra External ID (registro de usuarios)

Un tenant **externo** (External ID) permite que el paciente **cree su cuenta** desde el frontend
con el flujo de usuario "Registrarse e iniciar sesión". El tenant workforce de `01-entra-id.md`
no permite autoregistro. El código soporta ambos: se cambia solo con variables de entorno.

| Quién | Cómo obtiene su cuenta | Rol |
|---|---|---|
| Paciente | Se registra solo desde **Crear cuenta de paciente** | `Paciente` por defecto (`ENTRA_DEFAULT_ROLE`) |
| Médico | Se registra y el Admin le asigna el App Role `Medico` | `Medico` |
| Admin | Se registra y se le asigna el App Role `Admin` | `Admin` |

## 1. Crear el tenant externo

1. <https://entra.microsoft.com> → **Entra ID** → **Información general** → **Administrar inquilinos** → **Crear**.
2. Selecciona **Externo** → **Continuar** → **Prueba gratuita de 30 días** (no requiere suscripción).
3. Nombre del inquilino: `GestorCitas Pacientes` · Nombre de dominio: `gestorcitaspacientes` (o similar) · País: Chile → **Crear** (tarda unos minutos).
4. Cambia al nuevo tenant (engranaje → **Directorios + suscripciones**) y anota el **Tenant ID** y el **subdominio** (`<subdominio>.ciamlogin.com`).

## 2. App Registration de la API — `gestorcitas-api`

Igual que en `01-entra-id.md` §1.2:
- **Exponer una API** → URI por defecto `api://<client-id>` → scope `access_as_user`.
- **Roles de aplicación**: `Paciente`, `Medico`, `Admin` (tipo Usuarios/Grupos).
- **Manifiesto**: `"requestedAccessTokenVersion": 2`.
- **Configuración de token** → **Agregar notificación opcional** → tipo **Acceso** → `email`. **Obligatorio**: ms-usuarios identifica al usuario por su email.

## 3. App Registration del frontend — `gestorcitas-frontend`

- Plataforma **Aplicación de página única**: `http://localhost:5173` y `http://localhost:5173/login`.
- **Permisos de API** → gestorcitas-api → `access_as_user` → **Conceder consentimiento de administrador** (en External ID los usuarios no pueden consentir).

## 4. Flujo de usuario

1. **Identidades externas** → **Flujos de usuario** → **Nuevo flujo de usuario**.
2. Nombre `registro-inicio-sesion` · Proveedor **Correo electrónico con contraseña** · Atributos **Nombre para mostrar** → **Crear**.
3. Abre el flujo → **Aplicaciones** → **Agregar aplicación** → `gestorcitas-frontend`.

## 5. Usuarios de prueba

1. Cada integrante se registra desde `http://localhost:5173` → **Crear cuenta de paciente** (llega un código al correo).
   Con Gmail se puede usar un solo buzón: `tucorreo+admin@gmail.com`, `tucorreo+medico@gmail.com`, `tucorreo+paciente@gmail.com`.
2. **Aplicaciones empresariales** → `gestorcitas-api` → **Usuarios y grupos** → asigna `Admin` y `Medico` a quien corresponda.
3. Cierra sesión y vuelve a entrar para que el token traiga el nuevo rol.

El médico de demostración ("Dr. Elian Barra") se vincula por email. Si ya existe con el email antiguo,
actualízalo en Oracle (esquema USUARIOS):

```sql
UPDATE USUARIOS.USUARIOS SET EMAIL = '<email-del-medico-en-external-id>'
 WHERE EMAIL = '<email-anterior>';
COMMIT;
```

## 6. Variables

`.env` (Mac y EC2-A):

```
ENTRA_TENANT_ID=<tenant-id externo>
ENTRA_API_CLIENT_ID=<client-id gestorcitas-api>
ENTRA_API_APP_ID_URI=api://<client-id gestorcitas-api>
ENTRA_ISSUER=https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0
ENTRA_JWKS_URI=https://<subdominio>.ciamlogin.com/<tenant-id>/discovery/v2.0/keys
ENTRA_DEFAULT_ROLE=Paciente
```

`frontend/.env`:

```
VITE_ENTRA_CLIENT_ID=<client-id gestorcitas-frontend>
VITE_ENTRA_TENANT_ID=<tenant-id externo>
VITE_ENTRA_AUTHORITY=https://<subdominio>.ciamlogin.com/
VITE_API_SCOPE=api://<client-id gestorcitas-api>/access_as_user
VITE_ENTRA_SIGNUP=true
VITE_DEFAULT_ROLE=Paciente
```

Confirma issuer y JWKS en `https://<subdominio>.ciamlogin.com/<tenant-id>/v2.0/.well-known/openid-configuration`.

API Gateway (CloudShell), reutilizando la misma URL:

```
export ENTRA_TENANT_ID=<tenant-id externo> ENTRA_API_CLIENT_ID=<client-id api>
export ENTRA_ISSUER=https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0
export API_ID=<id actual> BFF_HOST=<DNS EC2-A> AWS_REGION=us-east-1
bash infra/aws/api-gateway.sh
```

**Volver al tenant workforce:** restaura los valores anteriores y deja vacíos `ENTRA_ISSUER`,
`ENTRA_JWKS_URI`, `VITE_ENTRA_AUTHORITY` y `VITE_ENTRA_SIGNUP`.
