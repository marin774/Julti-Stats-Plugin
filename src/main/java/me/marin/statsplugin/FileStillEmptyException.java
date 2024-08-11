package me.marin.statsplugin;

public class FileStillEmptyException extends RuntimeException {

    public FileStillEmptyException() {

    }

    public FileStillEmptyException(String message) {
        super(message);
    }

}
