package com.speedledger.filechangemonitor;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Use this to monitor a file for changes. Calls the OnFileChange method reference each time the file changes.
 */
public class FileChangeMonitor implements Runnable {
    private String directory;
    private String file;
    private OnFileChange action;
    private final OnLog logger;
    private int restoreOnFailureDelay;

    public FileChangeMonitor(String directory, String file, OnFileChange action, int restoreOnFailureDelay) {
        this(directory, file, action, new OnLog() {}, restoreOnFailureDelay);
    }

    public FileChangeMonitor(String directory, String file, OnFileChange action, OnLog logger, int restoreOnFailureDelay) {
        this.directory = directory;
        this.file = file;
        this.action = action;
        this.logger = logger;
        this.restoreOnFailureDelay = restoreOnFailureDelay;
    }

    public void run() {
        while (true) {
            final Path PATH = Paths.get(directory);
            FileSystem fileSystem = PATH.getFileSystem();

            try (WatchService service = fileSystem.newWatchService()) {
                logger.log(String.format("Watching file %s/%s for changes...", directory, file));
                action.fileChanged(directory, file);

                PATH.register(service, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
                while (true) {
                    final WatchKey key = service.take();
                    try {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            final Path changed = (Path) event.context();
                            if (changed.endsWith(file)) {
                                logger.log(String.format("Change detected in file %s/%s", directory, file));
                                action.fileChanged(directory, file);
                            }
                        }

                    } catch (Exception e) {
                        logger.log(String.format("Exception when watching file %s/%s, continuing to " +
                                "watch the file for changes", directory, file), e);
                    }

                    if (!key.reset()) {
                        break;
                    }
                }

            } catch (InterruptedException e) {
                logger.log(String.format("Watch of file %s/%s interrupted!, directory, file)", directory, file), e);
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                logger.log(String.format("EXCEPTION WHEN WATCHING FILE %s/%s", directory, file), e);
            }

            logger.log(String.format("THE WATCH OF FILE %s/%s BROKE, PLEASE RESTORE THE FILE! Will try to resume " +
                    "watch in %s seconds...", directory, file, restoreOnFailureDelay));

            try {
                Thread.sleep(1000 * restoreOnFailureDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * The action to take when the file changes on disk
     */
    @FunctionalInterface
    public interface OnFileChange {
        void fileChanged(String directory, String file) throws IOException;
    }

    public interface OnLog {
        final Logger logger = Logger.getLogger(FileChangeMonitor.class.toString());

        default void log(String message) {
            logger.log(Level.INFO, message);
        }

        default void log(String message, Throwable throwable) {
            logger.log(Level.SEVERE, message, throwable);
        }
    }
}
