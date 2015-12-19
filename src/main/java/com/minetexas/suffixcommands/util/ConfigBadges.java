package com.minetexas.suffixcommands.util;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigBadges {
	public String name;	
	public String badgeText;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigBadges> badges) {
		badges.clear();
		List<Map<?, ?>> badgeID = cfg.getMapList("badges");
		for (Map<?, ?> badge : badgeID) {
			ConfigBadges newBadge = new ConfigBadges();
			newBadge.name = (String)badge.get("name");
			newBadge.badgeText = (String)badge.get("badgeText");
			badges.put(newBadge.name, newBadge);
		}
		SCLog.info("Loaded "+badges.size()+" Badges.");
	}
}
