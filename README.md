# GestorCitaCloudNative

Sistema de **reserva de citas médicas** con arquitectura cloud native:
React + MSAL (Microsoft Entra ID) → AWS API Gateway (JWT) → BFF Spring Security → microservicios Spring Boot → Oracle Autonomous Database.

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
```

### Dónde corre cada componente

| Componente | Despliegue | Público |
|---|---|---|
| Frontend React | Local en el computador (`npm run dev`, `http://localhost:5173`) | No (llama a API Gateway) |
| API Gateway | AWS HTTP API · 25 rutas con autorizador JWT | Sí, HTTPS |
| BFF | **EC2-A** `gestorcitas-bff` · contenedor :8080 | Solo vía API Gateway |
| ms-citas · ms-usuarios · ms-catalogo | **EC2-B** `gestorcitas-ms` · contenedores :8082 · :8084 · :8083 | No: el Security Group solo acepta al BFF (IP privada) |
| Base de datos | Oracle Autonomous Database (OCI) | Solo la IP de EC2-B (ACL) |
| Identidad | Microsoft Entra ID | — |

El frontend corre en el computador y consume la API por la URL HTTPS de API Gateway (`https://…execute-api…amazonaws.com`). Despliegue en [docs/04-despliegue-ec2.md](docs/04-despliegue-ec2.md).

| Servicio | Responsabilidad |
|---|---|
| **ms-citas** (principal) | Reserva, cancelación, confirmación y atención de citas; disponibilidad horaria; indicadores |
| **ms-usuarios** | Pacientes, médicos y administradores: sincronización con Entra ID, perfiles, activación, pre-registro de médicos |
| **ms-catalogo** | Especialidades médicas y valor de la consulta |
| **bff** | Segunda validación JWT, autorización por rol, orquestación y reglas entre servicios |

## Actores y vistas

Cada rol tiene su **propio portal** con inicio, menú lateral y pantallas:

| Rol (App Role) | Portal | Pantallas |
|---|---|---|
| **Paciente** | `/paciente` | Inicio (indicadores + próximas citas), **Mi perfil** (RUT, fecha nac., previsión, teléfono), Reservar cita (especialidad → médico → fecha → bloque), Mis citas (cancelar) |
| **Medico** | `/medico` | Inicio (citas de hoy, por confirmar, atendidas), Mi agenda (confirmar, atender con observaciones), **Mi perfil** (especialidad, registro SIS, teléfono, presentación) |
| **Admin** | `/admin` | **Panel** (KPIs de usuarios, citas y catálogo), **Usuarios** (filtrar por rol, buscar, activar/desactivar, registrar y editar médicos), Especialidades (CRUD + valor), Citas (filtrar, cancelar) |
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
├── infra/
│   ├── db/oracle/     Scripts de esquemas para Autonomous DB (adb/) y Oracle local (local/)
│   └── aws/           Script API Gateway + user-data EC2
├── docs/              00-guia-paso-a-paso.md · 01-entra-id.md · 01b-entra-external-id.md · 03-oracle-autonomous-db.md · 04-despliegue-ec2.md
├── docker-compose.yml
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
│   └── citas/           Reserva, mis citas, agenda, administración de citas
├── hooks/  services/ (msal + http con MsalInterceptor)  stores/  types/  utils/
```

Guards en cadena (`app/router.tsx`): **AuthGuard** (sesión Entra ID) → **UsuarioGate** (sincronizado y activo) → **RoleGuard** (rol del portal).

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

## Ejecutar en local

> 📘 **Guía paso a paso (Entra ID → local → EC2 → API Gateway):** [docs/00-guia-paso-a-paso.md](docs/00-guia-paso-a-paso.md)

Requisitos: **Java 17+**, Node 20+, Docker. No necesitas Maven: usa `./mvnw`.

1. Configura Entra ID siguiendo **[docs/01-entra-id.md](docs/01-entra-id.md)**. Para que los pacientes **creen su cuenta** desde el frontend usa Entra External ID: **[docs/01b-entra-external-id.md](docs/01b-entra-external-id.md)**.
2. Crea la Autonomous Database y sus 3 esquemas siguiendo **[docs/03-oracle-autonomous-db.md](docs/03-oracle-autonomous-db.md)**.
3. Backend:
   ```bash
   cp .env.example .env        # DB_URL + esquemas Oracle, Tenant ID, Client ID de la API, email del médico demo
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
5. Despliegue en AWS (BFF en EC2-A, microservicios en EC2-B, detrás de API Gateway; el frontend sigue local): **[docs/04-despliegue-ec2.md](docs/04-despliegue-ec2.md)**.

### Datos de demostración

- **Especialidades:** Medicina General, Pediatría, Cardiología y Dermatología (con valor de consulta).
- **Médicos pre-registrados:** Dr. Elian Barra (`DEMO_MEDICO_EMAIL`), Dr. Matías Soto y Dr. Felipe Muñoz. Cuando `medico1` inicie sesión en Entra ID, queda vinculado al Dr. Elian Barra.
- **Pacientes y admins:** se crean solos en su primer login, según el App Role asignado.

## Tests

**Evidencia de rutas (Postman):** `infra/postman/GestorCitas.postman_collection.json` prueba las 25 rutas de API Gateway
con y sin token: 401 (sin token o token inválido, API Gateway), 200/201/204 (JSON de los microservicios),
403 (rol no permitido o ruta no definida, BFF). Instrucciones en la descripción de la colección.


```bash
./mvnw clean test          # reglas de negocio (Mockito), contexto JPA (H2) y seguridad del BFF (MockMvc)
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
