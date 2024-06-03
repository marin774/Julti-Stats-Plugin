# Julti Stats Plugin
A Julti plugin to make tracking stats even easier. This tracker works almost exactly the same as [pncakespoons's existing tracker](https://github.com/pncakespoon1/ResetTracker) and it works with [Specnr's Stats Website](https://reset-analytics-dev.vercel.app/).

## Installation
Download the latest version from the [Releases page](https://github.com/marin774/Julti-Stats-Plugin/releases), then drag and drop into your Julti plugins folder, and restart Julti.

## Migrating from pncakespoon's tracker
Once you installed the plugin and restarted Julti, follow these steps to migrate:
1. Open the "Plugins" tab in Julti.
2. Click on "Open Config" next to Stats Plugin.
3. Click on **`Import settings.json`**. Locate pncake's tracker folder, and find `settings.json` in the `data` folder.
4. Click on **`Import credentials.json`**. Locate pncake's tracker folder, and find `credentials.json`.
5. (**OPTIONAL**) Click on **`Import stats.csv`**. Locate pncake's tracker folder, and find `stats.csv` in the `data` folder.

> Note: Some stats that aren't being used currently won't be tracked with this plugin. This includes dropped gold, blocks mined, pearls thrown, deaths etc. They might be added in a future update.

## Other config options
![6ObZC6kmNQ](https://github.com/marin774/Julti-Stats-Plugin/assets/87690741/73b3e3ba-27ca-4b79-921b-6946bf996641)

### Open:
- You can view stats on the Stats Website, see your Google Sheet, and open settings and local stats file.

### Utility:
- Configure OBS overlay (%enters%, %nph% and %average% variables, you can format it however you want)

![iSeQZzSMn0](https://github.com/marin774/Julti-Stats-Plugin/assets/87690741/3aac42be-9c22-47f2-a21e-920ab4ec5021)
- Clear SpeedrunIGT records (done automatically on Julti startup if the `delete-old-records` setting is enabled)

### Debug:
- Reconnect to Google Sheets (if your credentials changed)
- Reload settings (if you manually edited `settings.json`)
- Start a new session (resets your current session AFTER you get any run)


### this is a wip, contact me on discord @marin774
