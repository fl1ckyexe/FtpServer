package org.example.ftp.server;

public class FtpServerMain {

    public static void main(String[] args) {
        trySetConsoleTitle();
        try {
            int ftpPort = Integer.getInteger("ftp.port", 2121);
            int adminPort = Integer.getInteger("admin.port", 9090);

            FtpServer server = new FtpServer(ftpPort, adminPort);
            server.start();
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                if (System.console() != null) {
                    System.out.println();
                    System.out.println("Server failed to start. Press Enter to exit...");
                    try (java.util.Scanner sc = new java.util.Scanner(System.in)) {
                        sc.nextLine();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void trySetConsoleTitle() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) return;
            new ProcessBuilder("cmd", "/c", "title", "FTP Server Logs")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception ignored) {
        }
    }
}
