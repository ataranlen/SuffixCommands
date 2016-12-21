package com.minetexas.suffixcommands.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.SuffixCommands;
import com.minetexas.suffixcommands.database.SQL;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;

public class SCSettings {

	public static SuffixCommands plugin;
	public static final String BADGE = "suffixcommands.badge.set";
	public static final String GROUP_BASE = "suffixcommands.badgegroup.";
	public static final String GROUPSHARE_BASE = "suffixcommands.sharegroup.";
	public static final String PERMISSION_BASE = "suffixcommands.badge.";
	public static final String PERMISSION_CHAT = "suffixcommands.chat.";
	public static final String PERMISSION_CREATE = "suffixcommands.createbadges";
	
	public static Boolean hasHerochat = false;
	
	public static FileConfiguration badgeConfig; /* badges.yml */
	public static Map<String, ConfigBadges> legacyBadges = new HashMap<String, ConfigBadges>();

	public static Map<String, Badge> badges = new HashMap<String, Badge>();
	
	public static void init(SuffixCommands plugin) throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration {
		SCSettings.plugin = plugin;

		SCSettings.hasHerochat = plugin.hasPlugin("Herochat");
		SCLog.debug("Herochat enabled? "+SCSettings.hasHerochat);
		loadConfigFiles();
		loadConfigObjects();
		try {
			SQL.initialize();
			SQL.initBadgeObjectTables();
			loadBadges();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadBadges() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			context = SQL.getGameConnection();		
			ps = context.prepareStatement("SELECT * FROM "+SQL.tb_prefix+Badge.TABLE_NAME);
			rs = ps.executeQuery();
			int count = 0;
			
			while(rs.next()) {
				try {
					Badge badge = new Badge(rs);
					badges.put(badge.getName(), badge);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			SCLog.info("Loaded "+count+" Badges from SQL");
		} finally {
			SQL.close(rs, ps, context);
		}
		
	}

	private static void loadConfigObjects() throws InvalidConfiguration {
		ConfigBadges.loadConfig(badgeConfig, legacyBadges);
	}
	
	public static void reloadBadgeConfigFile() throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration
	{
		legacyBadges.clear();
		badgeConfig = loadConfig("badges.yml");
		
		ConfigBadges.loadConfig(badgeConfig, legacyBadges);
	}
	
	public static FileConfiguration loadConfig(String filepath) throws FileNotFoundException, IOException, InvalidConfigurationException {

		File file = new File(plugin.getDataFolder().getPath()+"/"+filepath);
		if (!file.exists()) {
			SCLog.warning("Configuration file: '"+filepath+"' was missing. Streaming to disk from Jar.");
			streamResourceToDisk("/"+filepath);
		}
		
		SCLog.info("Loading Configuration file: "+filepath);
		// read the config.yml into memory
		YamlConfiguration cfg = new YamlConfiguration(); 
		cfg.load(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		return cfg;
	}
	
	public static String getStringBase(String path) throws InvalidConfiguration {
		return getString(badgeConfig, path);
	}
	
	public static int getIntegerBase(String path) throws InvalidConfiguration {
		return getInteger(badgeConfig, path);
	}
	
	public static String getString(FileConfiguration cfg, String path) throws InvalidConfiguration {
		String data = cfg.getString(path);
		if (data == null) {
			throw new InvalidConfiguration("Could not get configuration string "+path);
		}
		return data;
	}
	
	public static int getInteger(FileConfiguration cfg, String path) throws InvalidConfiguration {
		if (!cfg.contains(path)) {
			throw new InvalidConfiguration("Could not get configuration double "+path);
		}
		
		int data = cfg.getInt(path);
		return data;
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
