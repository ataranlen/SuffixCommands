package com.minetexas.suffixcommands.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.ChatterManager;
import com.dthielke.herochat.Herochat;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.util.ConfigBadges;

public class BadgeChatRouter {

	// return codes
	public static final int OK = 0;
	public static final int NOT_FOUND = 1;
	public static final int NO_ACCESS = 2;

	private static boolean useHerochat() {
		if (SCSettings.hasHerochat == true) {
			Herochat hc = Herochat.getPlugin();
			return hc != null && hc.isEnabled();
		}
		return false;
	}

	/**
	 * Routes a legacy badge chat message (ConfigBadges) to recipients and spies.
	 */
	public static int routeLegacyBadgeChat(final Player sender, final ConfigBadges legacy, final String messageText) {
		if (legacy == null) return NOT_FOUND;
		if (!sender.hasPermission(SCSettings.PERMISSION_CHAT + legacy.name) && !sender.isOp()) {
			return NO_ACCESS;
		}

		StringBuilder builder = new StringBuilder();
		String mainColor = ChatColor.translateAlternateColorCodes('&', legacy.chatColor);
		builder.append(mainColor + "[" + sender.getName()
			+ ChatColor.translateAlternateColorCodes('&', legacy.badgeText) + mainColor + "]" + mainColor);
		builder.append(" " + messageText);
		String message = builder.toString();
		SCLog.info(message);

		boolean hc = useHerochat();
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		Set<String> recipients = new HashSet<>();
		for (Player p : players) {
			if (hc) {
				ChatterManager cm = Herochat.getChatterManager();
				Chatter chatter = cm.getChatter(sender);
				Chatter chattee = cm.getChatter(p);
				if (chattee.isIgnoring(chatter)) {
					continue;
				}
			}
			if (p.hasPermission(SCSettings.PERMISSION_CHAT + legacy.name)) {
				p.sendMessage(message);
				recipients.add(p.getUniqueId().toString());
			}
		}

		for (String spyUUID : SCSettings.spyToggles) {
			try {
				if (recipients.contains(spyUUID)) continue;
				java.util.UUID id = java.util.UUID.fromString(spyUUID);
				Player spy = Bukkit.getPlayer(id);
				if (spy != null && spy.isOnline() && !spy.getUniqueId().toString().equals(sender.getUniqueId().toString())) {
					spy.sendMessage(SCColor.Yellow + "[SPY] " + message);
				}
			} catch (IllegalArgumentException e) {
				// ignore invalid uuid entries
			}
		}

		return OK;
	}

	/**
	 * Routes a new-style badge chat message (Badge) to recipients and spies.
	 */
	public static int routeBadgeChat(final Player sender, final Badge badge, final String messageText) {
		if (badge == null || badge.isDeleted()) return NOT_FOUND;
		String playerUUID = sender.getUniqueId().toString();
		if (!badge.canUseBadge(playerUUID) && !sender.isOp()) {
			return NO_ACCESS;
		}

		StringBuilder builder = new StringBuilder();
		String mainColor = ChatColor.translateAlternateColorCodes('&', badge.getChatColor());
		builder.append(mainColor + "[" + sender.getName()
			+ ChatColor.translateAlternateColorCodes('&', badge.getBadgeText()) + mainColor + "]" + mainColor);
		builder.append(" " + messageText);
		String message = builder.toString();
		SCLog.info(message);

		boolean hc = useHerochat();
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		Set<String> recipients = new HashSet<>();
		for (Player p : players) {
			if (hc) {
				ChatterManager cm = Herochat.getChatterManager();
				Chatter chatter = cm.getChatter(sender);
				Chatter chattee = cm.getChatter(p);
				if (chattee.isIgnoring(chatter)) {
					continue;
				}
			}
			if (badge.canUseBadge(p.getUniqueId().toString())) {
				p.sendMessage(message);
				recipients.add(p.getUniqueId().toString());
			}
		}

		for (String spyUUID : SCSettings.spyToggles) {
			try {
				if (recipients.contains(spyUUID)) continue;
				java.util.UUID id = java.util.UUID.fromString(spyUUID);
				Player spy = Bukkit.getPlayer(id);
				if (spy != null && spy.isOnline() && !spy.getUniqueId().toString().equals(sender.getUniqueId().toString())) {
					spy.sendMessage(SCColor.Yellow + "[SPY] " + message);
				}
			} catch (IllegalArgumentException e) {
				// ignore invalid uuid entries
			}
		}

		return OK;
	}
}
