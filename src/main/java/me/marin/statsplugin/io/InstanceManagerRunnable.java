package me.marin.statsplugin.io;

import me.marin.statsplugin.util.StatsPluginUtil;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates new rsg-attempts.txt watchers when new instances appear.
 */
public class InstanceManagerRunnable implements Runnable {

    public static final Map<String, RSGAttemptsWatcher> instanceWatcherMap = new HashMap<>();

    private final HashSet<String> previousOpenInstancePaths = new HashSet<>();

    @Override
    public void run() {
        Set<MinecraftInstance> currentOpenInstances = InstanceManager.getInstanceManager().getInstances().stream()
                .filter(i -> !i.checkWindowMissing())
                .collect(Collectors.toSet());

        Set<String> currentOpenInstancePaths = currentOpenInstances.stream()
                .map(i -> i.getPath().toString())
                .collect(Collectors.toSet());

        HashSet<String> closedInstancePaths = new HashSet<>(previousOpenInstancePaths);
        closedInstancePaths.removeAll(currentOpenInstancePaths);

        if (currentOpenInstancePaths.size() != previousOpenInstancePaths.size()) {
            Julti.log(Level.DEBUG, "previousOpenInstancePaths: " + previousOpenInstancePaths);
            Julti.log(Level.DEBUG, "currentActiveInstancePaths: " + currentOpenInstancePaths);
            Julti.log(Level.DEBUG, "closedInstancePaths: " + closedInstancePaths);
        }

        for (String closedInstancePath : closedInstancePaths) {
            // close old watchers (this instance was just closed)
            instanceWatcherMap.get(closedInstancePath).stop();
            instanceWatcherMap.remove(closedInstancePath);
            Julti.log(Level.DEBUG, "Closed a FileWatcher for instance: " + closedInstancePath);
        }

        for (MinecraftInstance instance : currentOpenInstances) {
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

                Julti.log(Level.DEBUG, "Starting a new FileWatcher for instance: " + instance.getName() + " (" + instance.getPath().toString() + ")");

                RSGAttemptsWatcher watcher = new RSGAttemptsWatcher(atumDirectory, wpStateoutPath);
                StatsPluginUtil.runAsync("rsg-attempts-watcher", watcher);
                instanceWatcherMap.put(path, watcher);
            }
        }

        previousOpenInstancePaths.clear();
        previousOpenInstancePaths.addAll(currentOpenInstancePaths);
    }

}
