package me.marin.statsplugin.io;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Works with single files and directories.
 * Directories will receive updates when a file in the directory has been modified.
 */
public abstract class FileWatcher implements Runnable {

    /**
     * Some programs will fire two ENTRY_MODIFY events without the file actually changing, and
     * <a href="https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event/25221600#25221600">this</a>
     * post explains it and addresses it, even though it's not the perfect solution.
     */
    private static final int DUPLICATE_UPDATE_PREVENTION_MS = 5;

    protected final File file;

    private WatchService watcher;
    private WatchKey watchKey;

    public FileWatcher(File file) {
        this.file = file;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Could not start FileWatcher:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    @Override
    public void run() {
        try {
            this.file.toPath().register(watcher, ENTRY_MODIFY);

            do {
                this.watchKey = watcher.take();

                Thread.sleep(DUPLICATE_UPDATE_PREVENTION_MS); // explained above

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    File updatedFile = new File(this.file, ev.context().toString());

                    if (event.kind() == ENTRY_MODIFY) {
                        if (updatedFile.length() > 0) {
                            handleFileUpdated(updatedFile);
                        }
                    }
                }
            } while (this.watchKey.reset());
        } catch (IOException | InterruptedException e) {
            Julti.log(Level.ERROR, "Error while reading:\n" + ExceptionUtil.toDetailedString(e));
        } catch (Exception e) {
            Julti.log(Level.DEBUG, "Unknown exception while reading:\n" + ExceptionUtil.toDetailedString(e));
        }
        Julti.log(Level.DEBUG, "FileWatcher was closed");
    }

    protected abstract void handleFileUpdated(File file);
    protected abstract void handleFileCreated(File file); //currently not used
    protected void stop() {
        watchKey.cancel();
    }

}
