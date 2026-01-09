# FTP server + browser admin UI

This repo contains:
- `ftp-server`: FTP server (port `2121`) + Admin HTTP API (port `9090`)
- `ftp-admin-ui`: legacy JavaFX admin client (kept for reference)

## Run (Windows / PowerShell)

### Recommended (opens a separate console window with server logs)

```powershell
.\run-server.ps1
```

Or (CMD):

```bat
run-server.cmd
```

### Manual

From the repo root (run from the `ftp-server` module to avoid running `exec:java` on the parent POM):

```powershell
cd ftp-server
..\mvnw.cmd -q -DskipTests exec:java "-Dexec.mainClass=org.example.ftp.server.FtpServerMain"
```

Ports:
- FTP server: `2121`
- Admin API + browser UI: `9090`

Open in browser:
- `http://localhost:9090/`

Admin token:
- Token is stored in the server DB and will be requested on admin UI open.

Note:
- On server start it will try to open the admin UI in your default browser automatically (best-effort).

## If port is already in use

If you see `java.net.BindException: Address already in use`, either stop the previous server instance, or run on a different port:

```powershell
cd ftp-server
..\mvnw.cmd -q -DskipTests exec:java "-Dexec.mainClass=org.example.ftp.server.FtpServerMain" "-Dadmin.port=9091"
```

## Admin API

All `/api/*` endpoints require:

```
Authorization: Bearer ADMIN_SECRET
```


