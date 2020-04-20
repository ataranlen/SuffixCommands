package com.minetexas.suffixcommands.util;

import java.util.List;
import java.util.Map;

import com.degoos.wetsponge.config.ConfigAccessor;

public class ConfigBadges {
	public String name;	
	public String badgeText;
	public String chatColor;
	
	public static void loadConfig(ConfigAccessor cfg, Map<String, ConfigBadges> badges) {
		badges.clear();
		@SuppressWarnings("unchecked")
		List<Map<?, ?>> badgeID = (List<Map<?, ?>>) cfg.getList("badges");
		for (Map<?, ?> badge : badgeID) {
			ConfigBadges newBadge = new ConfigBadges();
			newBadge.name = (String)badge.get("name");
			newBadge.badgeText = (String)badge.get("badgeText");
			newBadge.chatColor = (String)badge.get("color");
			badges.put(newBadge.name, newBadge);
		}
		SCLog.info("Loaded "+badges.size()+" Badges.");
	}
}