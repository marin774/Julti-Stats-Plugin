# Julti Stats Plugin
A Julti plugin to make tracking stats even easier. This tracker works almost the same as [pncakespoons's existing tracker](https://github.com/pncakespoon1/ResetTracker) and it works with the [Stats Website](https://reset-analytics-dev.vercel.app/).


## Installation
Download the latest version from the [Releases page](https://github.com/marin774/Julti-Stats-Plugin/releases), then drag and drop into your Julti plugins folder, and restart Julti.

## Migrating from pncakespoon's tracker
Once you installed the plugin and restarter Julti, follow these steps to migrate:
1. Open the "Plugins" tab in Julti.
2. Click on "Open Config" next to Stats Plugin.
3. Click on **`Import settings.json`**. Locate the old tracker, and find `settings.json` in the `data` folder.
4. Click on **`Import credentials.json`**. Locate the old tracker, and find `credentials.json`.
> Plugin will now attempt to connect to Google Sheets.
5. (**OPTIONAL**) Click on **`Import stats.csv`**. Locate the old tracker, and find `stats.csv` in the `data` folder.


> Note: Some stats that aren't being used won't be tracked with this plugin. This includes dropped gold, blocks mined, pearls thrown, deaths etc. They might be added in a future update.

## Other config options
![4j0yQOnSRT](https://github.com/marin774/Julti-Stats-Plugin/assets/87690741/87521026-e53d-43e3-9872-920a0abee86b)

- You can view your stats on the Stats Website (built by Specnr), open the Google Sheet, and settings/stats files.
- If you're having issues with Google Sheets, you can reload settings & reconnect to Google Sheets.
- You can also manually clear SpeedrunIGT records.


### this is a wip, contact me on discord @marin774
