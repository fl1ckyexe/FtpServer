param(
  [int]$FtpPort = 2121,
  [int]$AdminPort = 9090
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$cmd = "cd /d `"$root`" && cd ftp-server && ..\mvnw.cmd -q -DskipTests exec:java `"-Dexec.mainClass=org.example.ftp.server.FtpServerMain`" `"-Dftp.port=$FtpPort`" `"-Dadmin.port=$AdminPort`""

Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $cmd -WindowStyle Normal


