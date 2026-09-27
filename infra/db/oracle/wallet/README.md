# Wallet de Oracle Autonomous Database (opcional)

Solo se usa si tu Autonomous Database exige **mTLS** (conexión con wallet).
Si configuraste la conexión **TLS** (recomendado, ver `docs/03-oracle-autonomous-db.md`), no necesitas nada aquí.

1. Consola OCI → tu Autonomous Database → **Database connection** → **Download wallet** → define una contraseña.
2. Descomprime el `.zip` **en esta carpeta** (deben quedar `cwallet.sso`, `tnsnames.ora`, `sqlnet.ora`, etc.).
3. En `.env` usa:
   ```env
   DB_URL=jdbc:oracle:thin:@gestorcitas_low?TNS_ADMIN=/opt/oracle/wallet
   ```
   (reemplaza `gestorcitas` por el nombre de tu base; los alias disponibles están en `tnsnames.ora`).

⚠️ El wallet es una credencial: esta carpeta está en `.gitignore`, **no la subas a GitHub**.
