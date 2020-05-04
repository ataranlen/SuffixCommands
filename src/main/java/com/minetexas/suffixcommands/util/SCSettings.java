package com.minetexas.suffixcommands.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.degoos.wetsponge.WetSponge;
import com.degoos.wetsponge.command.WSCommandSource;
import com.degoos.wetsponge.config.ConfigAccessor;
import com.degoos.wetsponge.entity.living.player.WSPlayer;
import com.degoos.wetsponge.user.WSUser;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.SuffixCommands;
import com.minetexas.suffixcommands.database.SQL;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

public class SCSettings {

    private static ConfigAccessor badgeConfig; /* badges.yml */

	public static SuffixCommands plugin;
	public static final String BADGE = "suffixcommands.badge.set";
	public static final String GROUP_BASE = "suffixcommands.badgegroup.";
	public static final String GROUPSHARE_BASE = "suffixcommands.sharegroup.";
	public static final String PERMISSION_BASE = "suffixcommands.badge.";
	public static final String PERMISSION_CHAT = "suffixcommands.chat.";
	public static final String PERMISSION_CREATE = "suffixcommands.createbadges";
	
	public static Boolean hasLP = false;
	
	public static LuckPerms luckPermsAPI = LuckPermsProvider.get();
	
	public static Map<String, ConfigBadges> legacyBadges = new HashMap<String, ConfigBadges>();

	public static Map<String, Badge> badges = new HashMap<String, Badge>();
	
	public static void init(SuffixCommands plugin) throws FileNotFoundException, IOException, InvalidConfiguration {
		SCSettings.plugin = plugin;

		SCSettings.hasLP = true;
		SCLog.debug("LuckPerms enabled? "+SCSettings.hasLP);
		
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
	
	public static void reloadBadgeConfigFile() throws FileNotFoundException, IOException, InvalidConfiguration
	{
		legacyBadges.clear();
		badgeConfig = loadConfig("badges.yml");
		
		ConfigBadges.loadConfig(badgeConfig, legacyBadges);
	}
	
	public static ConfigAccessor loadConfig(String filepath) throws FileNotFoundException, IOException {

		File file = new File(plugin.getDataFolder().getPath()+"/"+filepath);
		if (!file.exists()) {
			SCLog.warning("Configuration file: '"+filepath+"' was missing. Streaming to disk from Jar.");
			streamResourceToDisk("/"+filepath);
		}
		
		SCLog.info("Loading Configuration file: "+filepath);
		// read the config.yml into memory
		
		return new ConfigAccessor(new File(plugin.getDataFolder(), filepath));
	}
	
	public static String getStringBase(String path) throws InvalidConfiguration {
		return getString(badgeConfig, path);
	}
	
	public static int getIntegerBase(String path) throws InvalidConfiguration {
		return getInteger(badgeConfig, path);
	}
	
	public static String getString(ConfigAccessor cfg, String path) throws InvalidConfiguration {
		String data = cfg.getString(path);
		if (data == null) {
			throw new InvalidConfiguration("Could not get configuration string "+path);
		}
		return data;
	}
	
	public static int getInteger(ConfigAccessor cfg, String path) throws InvalidConfiguration {
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
	
	private static void loadConfigFiles() throws FileNotFoundException, IOException {
		badgeConfig = loadConfig("badges.yml");
	}
	
	public WSUser loadUser(WSPlayer player) {
	    // assert that the player is online
	    if (!player.isOnline()) {
	        throw new IllegalStateException("Player is offline");
	    }
	    return (WSUser) luckPermsAPI.getUserManager().getUser(player.getUniqueId());
	}
	
	public static String getSuffix(User user) {
		ContextManager contextManager = LuckPermsProvider.get().getContextManager();
	    ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);

	    CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.contextual(contextSet));

	    return metaData.getSuffix();
	}
	
	public static void setSuffix(User user, String suffix) {
		WSCommandSource server = WetSponge.getServer().getConsole();
		server.performCommand("lp user "+ user.getUsername() +" meta clear suffixes");
		server.performCommand("lp user "+ user.getUsername() +" meta setsuffix 150 \"" + suffix + "\"");
	}
	
	public static void setSuffix(WSPlayer player, String suffix) {
		User user = SCSettings.luckPermsAPI.getUserManager().getUser(player.getUniqueId());
		setSuffix(user, suffix);
	}
	
}
