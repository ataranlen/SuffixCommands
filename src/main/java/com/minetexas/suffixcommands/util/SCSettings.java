package com.minetexas.suffixcommands.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.minetexas.suffixcommands.SuffixCommands;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;

public class SCSettings {

	public static SuffixCommands plugin;
	public static final String BADGE = "suffixcommands.badge.set";
	public static final String GROUP_BASE = "suffixcommands.badgegroup.";
	public static final String PERMISSION_BASE = "suffixcommands.badge.";
	public static final String PERMISSION_CHAT = "suffixcommands.chat.";
	
	public static Boolean hasHerochat = false;
	
	public static FileConfiguration badgeConfig; /* badges.yml */
	public static Map<String, ConfigBadges> badges = new HashMap<String, ConfigBadges>();
	
	public static void init(SuffixCommands plugin) throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration {
		SCSettings.plugin = plugin;

		SCSettings.hasHerochat = plugin.hasPlugin("Herochat");
		SCLog.debug("Herochat enabled? "+SCSettings.hasHerochat);
		loadConfigFiles();
		loadConfigObjects();
	}
	
	private static void loadConfigObjects() throws InvalidConfiguration {
		ConfigBadges.loadConfig(badgeConfig, badges);
	}
	
	public static void reloadBadgeConfigFile() throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration
	{
		badges.clear();
		badgeConfig = loadConfig("badges.yml");
		ConfigBadges.loadConfig(badgeConfig, badges);
	}
	
	public static FileConfiguration loadConfig(String filepath) throws FileNotFoundException, IOException, InvalidConfigurationException {

		File file = new File(plugin.getDataFolder().getPath()+"/data/"+filepath);
		if (!file.exists()) {
			SCLog.warning("Configuration file: '"+filepath+"' was missing. Streaming to disk from Jar.");
			streamResourceToDisk("/data/"+filepath);
		}
		
		SCLog.info("Loading Configuration file: "+filepath);
		// read the config.yml into memory
		YamlConfiguration cfg = new YamlConfiguration(); 
		cfg.load(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		return cfg;
	}
	
	public static void streamResourceToDisk(String filepath) throws IOException {
		URL inputUrl = plugin.getClass().getResource(filepath);
		File dest = new File(plugin.getDataFolder().getPath()+filepath);
		FileUtils.copyURLToFile(inputUrl, dest);
	}
	
	private static void loadConfigFiles() throws FileNotFoundException, IOException, InvalidConfigurationException {
		badgeConfig = loadConfig("badges.yml");
	}
}
