package org.example.ftp.common.protocol;

public final class Responses {

    private Responses() {}


    public static FtpResponse ok(int code, String msg) {
        return FtpResponse.builder()
                .code(code)
                .message(msg)
                .build();
    }

    public static FtpResponse error(int code, String msg) {
        return FtpResponse.builder()
                .code(code)
                .message(msg)
                .build();
    }


    public static FtpResponse needLogin() {
        return error(530, "Please login first.");
    }

    public static FtpResponse loginWithUserFirst() {
        return error(530, "Login with USER first.");
    }

    public static FtpResponse loginIncorrect() {
        return error(530, "Login incorrect.");
    }

    public static FtpResponse permissionDenied() {
        return error(550, "Permission denied.");
    }

    public static FtpResponse accessDenied() {
        return error(550, "Access denied.");
    }

    public static FtpResponse usePasvFirst() {
        return error(425, "Use PASV first.");
    }

    public static FtpResponse transferComplete() {
        return ok(226, "Transfer complete.");
    }

    public static FtpResponse directorySendOk() {
        return ok(226, "Directory send OK.");
    }

    public static FtpResponse connectionClosedTransferAborted() {
        return error(426, "Connection closed; transfer aborted.");
    }

    public static FtpResponse requestedActionAbortedLocalError() {
        return error(451, "Requested action aborted. Local error.");
    }

    public static FtpResponse missingFileName() {
        return error(501, "Missing file name.");
    }

    public static FtpResponse missingDirectoryName() {
        return error(501, "Directory name required.");
    }

    public static FtpResponse syntaxErrorInParameters() {
        return error(501, "Syntax error in parameters.");
    }

    public static FtpResponse fileNotFound() {
        return error(550, "File not found.");
    }

    public static FtpResponse notImplemented() {
        return error(502, "Command not implemented.");
    }

    public static FtpResponse emptyCommand() {
        return error(500, "Empty command");
    }

    public static FtpResponse goodbye() {
        return ok(221, "Goodbye.");
    }

    public static FtpResponse directoryChanged() {
        return ok(250, "Directory successfully changed.");
    }

    public static FtpResponse fileDeleted() {
        return ok(250, "File deleted.");
    }

    public static FtpResponse directoryDeleted() {
        return ok(250, "Directory deleted.");
    }
}