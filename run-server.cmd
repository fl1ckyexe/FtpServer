@echo off
setlocal

REM Opens a dedicated console window for server logs, and keeps it open.
REM Admin UI will still open in the browser automatically (server feature).

set "ROOT=%~dp0"

start "FTP Server (logs)" cmd /k ^
  "cd /d \"%ROOT%\" && cd ftp-server && ..\\mvnw.cmd -q -DskipTests exec:java \"-Dexec.mainClass=org.example.ftp.server.FtpServerMain\""

endlocal


