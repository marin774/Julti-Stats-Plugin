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

    protected final String name;
    protected final File file;

    private WatchService watchService;

    public FileWatcher(String name, File file) {
        this.name = name;
        this.file = file;
    }

    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Could not start WatchService:\n" + ExceptionUtil.toDetailedString(e));
            return;
        }
        try {
            this.file.toPath().register(watchService, ENTRY_MODIFY);

            WatchKey watchKey;
            do {
                watchKey = watchService.take();

                Thread.sleep(DUPLICATE_UPDATE_PREVENTION_MS); // explained above

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    File updatedFile = new File(this.file, ev.context().toString());

                    if (event.kind() == ENTRY_MODIFY) {
                        if (updatedFile.length() > 0) {
                            try {
                                handleFileUpdated(updatedFile);
                            } catch (Exception e) {
                                Julti.log(Level.ERROR, "Unhandled exception in '" + this.name + "':\n" + ExceptionUtil.toDetailedString(e));
                            }
                        }
                    }
                }
            } while (watchKey.reset());
        } catch (ClosedWatchServiceException ignored) {
            // when stop method gets called, ClosedWatchServiceException is thrown, and file watcher should stop.
        } catch (IOException | InterruptedException e) {
            Julti.log(Level.ERROR, "Error while reading:\n" + ExceptionUtil.toDetailedString(e));
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Unknown exception while reading:\n" + ExceptionUtil.toDetailedString(e));
        }
        Julti.log(Level.DEBUG, "FileWatcher was closed " + name);
    }

    protected abstract void handleFileUpdated(File file);
    protected abstract void handleFileCreated(File file); //currently not used
    protected void stop() {
        try {
            watchService.close();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Could not stop WatchService:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

}
