package me.marin.statsplugin.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public abstract class FileWatcher implements Runnable {

    protected final File file;
    private final boolean isDirectory;

    protected final File parentDirectory;

    private final WatchService watcher;

    public FileWatcher(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.parentDirectory = file.getParentFile();

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            File directory = isDirectory ? this.file : this.file.getParentFile();
            directory.toPath().register(watcher, ENTRY_MODIFY, ENTRY_CREATE);

            //noinspection InfiniteLoopStatement
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    File file = new File(directory, ev.context().toString());

                    if (event.kind() == ENTRY_MODIFY) {
                        if (file.length() > 0) {
                            handleFileUpdated(file);
                        }
                    } else {
                        handleFileCreated(file);
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (ClosedWatchServiceException ignored) {
            /*
            Exception is thrown because WatchService#take is still waiting, but WatchService#close was called.
            It should be ignored.
            */
        }
    }

    protected abstract void handleFileUpdated(File file);
    protected abstract void handleFileCreated(File file);
    protected void stop() {
        try {
            watcher.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
