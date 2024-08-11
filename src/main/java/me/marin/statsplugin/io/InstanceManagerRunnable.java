package me.marin.statsplugin.io;

import me.marin.statsplugin.StatsPluginUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates new rsg-attempts.txt watchers when new instances appear.
 * <p>
 * Ideally this shouldn't even be a thing because SeedQueue is single-instance, but
 * it's used as a safety net if you relaunch instances or have other instances in background etc.
 */
public class InstanceManagerRunnable implements Runnable {

    public static final Map<String, RSGAttemptsWatcher> instanceWatcherMap = new HashMap<>();

    private final HashSet<String> previousActiveInstancePaths = new HashSet<>();

    @Override
    public void run() {
        HashSet<String> currentActiveInstancePaths = InstanceManager.getInstanceManager().getInstances().stream().map(i -> i.getPath().toString()).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> closedInstancePaths = new HashSet<>(previousActiveInstancePaths);
        closedInstancePaths.removeAll(currentActiveInstancePaths);

        for (String closedInstancePath : closedInstancePaths) {
            // close old watchers (this instance was just closed)
            instanceWatcherMap.get(closedInstancePath).stop();
            instanceWatcherMap.remove(closedInstancePath);
            Julti.log(Level.DEBUG, "Closed a FileWatcher for instance: " + closedInstancePath);
        }

        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            String path = instance.getPath().toString();
            if (!instanceWatcherMap.containsKey(path)) {
                Path rsgAttemptsPath = Paths.get(instance.getPath().toString(), "config", "mcsr", "atum", "rsg-attempts.txt");

                // Wait until the file exists (if they just set up or updated Atum OR if it's a misc instance, this won't exist)
                if (!Files.exists(rsgAttemptsPath)) {
                    continue;
                }

                // start a new watcher
                Path atumDirectory = Paths.get(instance.getPath().toString(), "config", "mcsr", "atum");
                Path wpStateoutPath = Paths.get(instance.getPath().toString(), "wpstateout.txt");

                Julti.log(Level.DEBUG, "Starting a new FileWatcher for instance: " + instance.getName() + "(" + instance.getPath().toString() + ")");

                RSGAttemptsWatcher watcher = new RSGAttemptsWatcher(instance.getHwnd(), atumDirectory, wpStateoutPath);
                StatsPluginUtil.runAsync("rsg-attempts-watcher", watcher);
                instanceWatcherMap.put(path, watcher);
            }
        }

        previousActiveInstancePaths.clear();
        previousActiveInstancePaths.addAll(currentActiveInstancePaths);
    }

}
