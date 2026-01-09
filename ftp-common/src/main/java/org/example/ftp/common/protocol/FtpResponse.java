package org.example.ftp.common.protocol;

import java.util.ArrayList;
import java.util.List;


public final class FtpResponse {

    private final int code;
    private final List<String> lines;

    private FtpResponse(int code, List<String> lines) {
        this.code = code;
        this.lines = List.copyOf(lines);
    }

    public int getCode() {
        return code;
    }

    public List<String> getLines() {
        return lines;
    }

    public String toProtocolString() {
        StringBuilder sb = new StringBuilder();
        if (lines.size() == 1) {
            sb.append(code).append(" ").append(lines.get(0)).append("\r\n");
        } else {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    sb.append(code).append("-").append(lines.get(i)).append("\r\n");
                } else if (i == lines.size() - 1) {
                    sb.append(code).append(" ").append(lines.get(i)).append("\r\n");
                } else {
                    sb.append(lines.get(i)).append("\r\n");
                }
            }
        }
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }


    public static FtpResponse ok(int code, String message) {
        return builder()
                .code(code)
                .message(message)
                .build();
    }

    public static FtpResponse error(int code, String message) {
        return builder()
                .code(code)
                .message(message)
                .build();
    }

    public static FtpResponse needLogin() {
        return error(530, "Please login first.");
    }

    public static FtpResponse usePasvFirst() {
        return error(425, "Use PASV first.");
    }

    public static FtpResponse notImplemented() {
        return error(502, "Command not implemented.");
    }

    public static FtpResponse emptyCommand() {
        return error(500, "Empty command");
    }

    
    public static final class Builder {
        private Integer code;
        private final List<String> lines = new ArrayList<>();

        
        public Builder code(int code) {
            this.code = code;
            return this;
        }

        
        public Builder message(String message) {
            if (message != null) {
                this.lines.add(message);
            }
            return this;
        }

        
        public Builder line(String line) {
            if (line != null) {
                this.lines.add(line);
            }
            return this;
        }

        
        public FtpResponse build() {
            if (code == null) {
                throw new IllegalStateException("FTP reply code is required");
            }
            if (lines.isEmpty()) {
                lines.add("");
            }
            return new FtpResponse(code, lines);
        }
    }
}