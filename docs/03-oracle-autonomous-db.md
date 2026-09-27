# 3. Oracle Autonomous Database (persistencia)

**1 instancia** de Autonomous Database en Oracle Cloud (OCI), con **1 esquema por microservicio**:

| Microservicio | Esquema (usuario Oracle) | Tablas (las crea el microservicio al arrancar) |
|---|---|---|
| ms-catalogo | `CATALOGO` | `ESPECIALIDADES` |
| ms-usuarios | `USUARIOS` | `USUARIOS`, `USUARIO_ROLES`, `PERFILES_PACIENTE`, `PERFILES_MEDICO` |
| ms-citas | `CITAS` | `CITAS` |

```
AWS EC2 (Docker)                          Oracle Cloud
 ├── ms-catalogo ──── TLS :1521 ────►  ┌─────────────────────────────┐
 ├── ms-usuarios ──── TLS :1521 ────►  │ Autonomous Database         │
 └── ms-citas    ──── TLS :1521 ────►  │  CATALOGO · USUARIOS · CITAS│
                                       └─────────────────────────────┘
```

Cada microservicio se conecta **solo con su usuario**, así que no puede leer ni escribir los datos de otro (database-per-service).

---

## 3.1 Cuenta de Oracle Cloud

1. Entra a https://www.oracle.com/cloud/free/ → **Start for free**.
2. Completa el registro. Pide una tarjeta solo para verificar identidad: los recursos **Always Free** no se cobran.
3. **Home region**: elige **Chile Central (Santiago)** `sa-santiago-1` o **Brazil East (São Paulo)**. No se puede cambiar después, y la Always Free queda en esa región.
4. Espera el correo de "Your account is ready" (a veces tarda unos minutos) e inicia sesión en https://cloud.oracle.com.

## 3.2 Crear la Autonomous Database

1. Menú ☰ → **Oracle Database** → **Autonomous Database** → **Create Autonomous Database**.
2. Completa:

| Campo | Valor |
|---|---|
| Compartment | el raíz (tu tenancy) |
| Display name | `gestorcitas` |
| Database name | `GESTORCITAS` |
| Workload type | **Transaction Processing** |
| Deployment type | **Serverless** |
| **Always Free** | ✅ **activado** (si no aparece, cambia a la región Home) |
| Database version | 19c o 23ai (cualquiera sirve) |
| ADMIN password | 12–30 caracteres, con mayúscula, minúscula y número (ej. `<CLAVE_ADMIN>`). **Anótala.** |
| Network access | **Secure access from allowed IPs and VCNs only** |
| Access control list | Tipo **IP address** → **Add my IP address** (la IP de tu casa). La IP de la EC2 se agrega en la Fase 3 |
| Require mutual TLS (mTLS) | ❌ **desmarcado** → permite conectarse por TLS sin wallet |

3. **Create Autonomous Database**. En 2–5 minutos el estado pasa a **Available** (verde).

> ¿Te obliga a usar mTLS? Déjalo así y usa el wallet (ver `infra/db/oracle/wallet/README.md`). El código ya soporta las dos formas.

## 3.3 Obtener la cadena de conexión

1. En la página de la base → **Database connection**.
2. **TLS authentication** → elige **TLS**.
3. En la tabla, copia el *Connection string* de **`gestorcitas_low`**. `_low` es el perfil con más sesiones disponibles, el indicado para aplicaciones.
4. En el `.env`, **antepón `jdbc:oracle:thin:@`** y déjalo entre comillas simples:

```env
DB_URL='jdbc:oracle:thin:@(description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.sa-santiago-1.oraclecloud.com))(connect_data=(service_name=abc123_gestorcitas_low.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))'
```

## 3.4 Crear los 3 esquemas

1. Página de la base → **Database actions** → **SQL**. Entras como `ADMIN`.
2. Abre `infra/db/oracle/adb/01-crear-esquemas.sql`, copia todo y pégalo en el editor.
3. Pulsa **Run Script** (ícono de hoja con ▶, o **F5**). *No* uses "Run Statement", porque ese ejecuta solo una línea.
4. La consulta final debe listar `CATALOGO`, `USUARIOS` y `CITAS` con estado `OPEN`.

Si cambiaste las contraseñas en el script, pon las mismas en el `.env`:

```env
CATALOGO_DB_USER=CATALOGO
CATALOGO_DB_PASSWORD=<CLAVE_CATALOGO>
USUARIOS_DB_USER=USUARIOS
USUARIOS_DB_PASSWORD=<CLAVE_USUARIOS>
CITAS_DB_USER=CITAS
CITAS_DB_PASSWORD=<CLAVE_CITAS>
```

## 3.5 Primer arranque y verificación

```bash
docker compose up -d --build
docker compose logs -f ms-catalogo    # busca "Started MsCatalogoApplication" y "Catálogo inicial de especialidades cargado"
```

Luego, en **Database actions → SQL** (como ADMIN), ejecuta `infra/db/oracle/adb/02-verificar-datos.sql` con **Run Script**. Verás:
- las tablas de cada esquema;
- las 4 especialidades en `CATALOGO.ESPECIALIDADES`;
- los 3 médicos pre-registrados en `USUARIOS.USUARIOS`;
- las citas en `CITAS.CITAS` (a medida que se reserven).

Esa consulta sirve como evidencia de persistencia en la presentación.

## 3.6 Permitir la conexión desde la EC2 (Fase 3 de la guía)

Cuando tengas la **Elastic IP** de la EC2:

1. Página de la base → **Network** → **Access control list** → **Edit**.
2. **Add access control rule** → IP address → `<EC2_IP>` → **Save**.
3. Mientras se actualiza (1–2 min) el estado queda en *Updating*.

Si tu IP de casa cambia (por ejemplo, si reinicias el router), vuelve a agregarla aquí para trabajar en local.

## 3.7 Desarrollo local sin Oracle Cloud (opcional)

Si todavía no tienes la Autonomous DB, o no tienes internet, puedes usar Oracle Database Free en Docker con los mismos 3 esquemas:

```env
# .env
DB_URL=jdbc:oracle:thin:@//oracle-local:1521/FREEPDB1
```

```bash
docker compose --profile oracle-local up -d --build
```

- La primera vez tarda 2–4 minutos en crear la base. Mientras tanto los microservicios se reinician solos hasta que la base responde.
- Desde tu Mac puedes conectarte con SQL Developer o DBeaver: `localhost:1521`, servicio `FREEPDB1`, usuario `CATALOGO`, `USUARIOS` o `CITAS`.
- Para borrar los datos locales: `docker compose --profile oracle-local down -v`.

## Notas de la capa Always Free

- La base **se detiene sola tras 7 días sin uso**. Actívala desde la consola con **More actions → Start** antes de trabajar o de presentar.
- Tiene un límite bajo de sesiones simultáneas, por eso cada microservicio usa como máximo 4 conexiones (`DB_POOL_SIZE=4`). Si trabajas en local y en la EC2 a la vez contra la misma base, baja el valor a 2 en ambos lados.
- 20 GB de almacenamiento: más que suficiente.

## Solución de problemas

| Error en los logs | Causa | Solución |
|---|---|---|
| `ORA-12506` / `listener rejected connection` / `IO Error: Connection reset` | Tu IP (o la de la EC2) no está en la lista de acceso | Agrégala en **Network → Access control list** |
| `IO Error` o problemas de handshake SSL con TLS | La base exige mTLS | Desmarca *Require mTLS* (con la lista de acceso configurada) o usa el wallet |
| `ORA-01017 invalid username/password` | Contraseña distinta entre el script SQL y el `.env` | Corrige el `.env`, o en SQL: `ALTER USER CITAS IDENTIFIED BY "NuevaClave#2026x";` |
| `ORA-28000 account is locked` | Demasiados intentos fallidos | Como ADMIN: `ALTER USER CITAS ACCOUNT UNLOCK;` |
| `ORA-01950 no privileges on tablespace` | Falta la cuota | Como ADMIN: `ALTER USER CITAS QUOTA UNLIMITED ON DATA;` |
| `ORA-00018 maximum number of sessions exceeded` | Demasiadas conexiones | Baja `DB_POOL_SIZE` y reinicia con `docker compose up -d` |
| La base no responde | Se detuvo por inactividad | Consola → **Start** |
