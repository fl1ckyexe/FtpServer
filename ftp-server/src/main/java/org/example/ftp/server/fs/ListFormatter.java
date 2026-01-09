package org.example.ftp.server.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ListFormatter {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private ListFormatter() {}

    public static String format(Path path) {
        try {
            boolean isDir = Files.isDirectory(path);
            long size = isDir ? 0 : Files.size(path);

            FileTime time = Files.getLastModifiedTime(path);
            String date = DATE.format(Instant.ofEpochMilli(time.toMillis()));

            String perms = isDir ? "drwxr-xr-x" : "-rw-r--r--";

            return String.format(
                    "%s 1 user group %8d %s %s",
                    perms,
                    size,
                    date,
                    path.getFileName().toString()
            );
        } catch (Exception e) {
            return path.getFileName().toString();
        }
    }

    
    public static String formatDirectory(String name) {
        String date = DATE.format(Instant.now());
        return String.format(
                "drwxr-xr-x 1 user group %8d %s %s",
                0,
                date,
                name
        );
    }
}
