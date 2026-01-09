package org.example.ftp.common.protocol;

public enum FtpReplyCode {

    SERVICE_READY(220),
    USERNAME_OK(331),
    USER_LOGGED_IN(230),
    NOT_LOGGED_IN(530),
    FILE_ACTION_OK(250),
    COMMAND_NOT_IMPLEMENTED(502),
    PATHNAME_CREATED(257);

    private final int code;

    FtpReplyCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
