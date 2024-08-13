package me.marin.statsplugin.util;

public class FileStillEmptyException extends RuntimeException {

    public FileStillEmptyException() {

    }

    public FileStillEmptyException(String message) {
        super(message);
    }

}
