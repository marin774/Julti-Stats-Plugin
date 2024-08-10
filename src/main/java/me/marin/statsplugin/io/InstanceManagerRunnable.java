package me.marin.statsplugin.io;

import me.marin.statsplugin.StatsPluginUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

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
                // start a new watcher (this instance was just launched)
                RSGAttemptsWatcher watcher = new RSGAttemptsWatcher(Paths.get(instance.getPath().toString(), "config", "mcsr", "atum"));
                Julti.log(Level.DEBUG, "Starting a new FileWatcher for instance: " + instance.getName() + "(" + instance.getPath().toString() + ")");
                StatsPluginUtil.runAsync("rsg-attempts-watcher", watcher);
                instanceWatcherMap.put(path, watcher);
            }
        }

        previousActiveInstancePaths.clear();
        previousActiveInstancePaths.addAll(currentActiveInstancePaths);
    }

}
