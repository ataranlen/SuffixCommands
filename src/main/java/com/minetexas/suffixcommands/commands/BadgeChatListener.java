package com.minetexas.suffixcommands.commands;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.ChatterManager;
import com.dthielke.herochat.Herochat;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class BadgeChatListener implements Listener {

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player sender = event.getPlayer();
		String playerUUID = sender.getUniqueId().toString();
		String badgeName = SCSettings.bcToggles.get(playerUUID);
		if (badgeName == null) {
			return; // not in badge-chat toggle mode
		}

		// Cancel default chat and route to badge chat. Run sync task for broadcasting safe server interaction.
		event.setCancelled(true);
		final String messageText = event.getMessage();
		final String badgeKey = badgeName;
		Bukkit.getScheduler().runTask(SCSettings.plugin, new Runnable() {
			@Override
			public void run() {
				// Attempt to find either new-style badge or legacy badge config
				Badge badge = SCSettings.badges.get(badgeKey);
				if (badge == null) {
					ConfigBadges legacyBadge = SCSettings.legacyBadges.get(badgeKey);
					if (legacyBadge == null) {
						sender.sendMessage(SCColor.Red + "Invalid Badge configured for your /bc setting. Badge chat disabled.");
						SCSettings.bcToggles.remove(playerUUID);
						return;
					}

					// Delegate routing to BadgeChatRouter
					int rc = com.minetexas.suffixcommands.util.BadgeChatRouter.routeLegacyBadgeChat(sender, legacyBadge, messageText);
					if (rc == com.minetexas.suffixcommands.util.BadgeChatRouter.NOT_FOUND) {
						sender.sendMessage(SCColor.Red + "Invalid Badge configured for your /bc setting. Badge chat disabled.");
						SCSettings.bcToggles.remove(playerUUID);
						return;
					} else if (rc == com.minetexas.suffixcommands.util.BadgeChatRouter.NO_ACCESS) {
						sender.sendMessage(SCColor.Red + "You don't have access to the '" + legacyBadge.name + "' Badge Group. Badge chat disabled.");
						SCSettings.bcToggles.remove(playerUUID);
						return;
					}
					return;
				}

				// Delegate routing to BadgeChatRouter for new-style badge
				int rc2 = com.minetexas.suffixcommands.util.BadgeChatRouter.routeBadgeChat(sender, badge, messageText);
				if (rc2 == com.minetexas.suffixcommands.util.BadgeChatRouter.NO_ACCESS) {
					sender.sendMessage(SCColor.Red + "You don't have access to the '" + badge.getName() + "' Badge Group. Badge chat disabled.");
					SCSettings.bcToggles.remove(playerUUID);
					return;
				}
			}
		});
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playerUUID = player.getUniqueId().toString();

		// Check spy toggle on login
		if (SCSettings.spyToggles.contains(playerUUID)) {
			if (!player.isOp() && !player.hasPermission("suffixcommands.badge.spy")) {
				SCSettings.spyToggles.remove(playerUUID);
				player.sendMessage(SCColor.Red + "Badge chat spy was removed because you no longer have permission.");
			} else {
				player.sendMessage(SCColor.Green + "Badge chat spy is ENABLED. Use /chattoggle spy to disable.");
			}
		}

		String badgeName = SCSettings.bcToggles.get(playerUUID);
		if (badgeName == null) return;

		// If toggle exists, validate and notify as-if they just enabled it
		Badge badge = SCSettings.badges.get(badgeName);
		if (badge == null) {
			ConfigBadges legacy = SCSettings.legacyBadges.get(badgeName);
			if (legacy == null) {
				player.sendMessage(SCColor.Red + "Your badge-chat was removed because the badge no longer exists.");
				SCSettings.bcToggles.remove(playerUUID);
				return;
			}
			if (!player.hasPermission(SCSettings.PERMISSION_CHAT + legacy.name) && !player.isOp()) {
				player.sendMessage(SCColor.Red + "Your badge-chat was removed because you no longer have access to '" + legacy.name + "'.");
				SCSettings.bcToggles.remove(playerUUID);
				return;
			}
			player.sendMessage(SCColor.Green + "Badge chat enabled for '" + legacy.name + "'. Use /bc or /chat with no args to turn it off.");
			return;
		}

		// new-style badge
		if (!badge.canUseBadge(playerUUID) && !player.isOp()) {
			player.sendMessage(SCColor.Red + "Your badge-chat was removed because you no longer have access to '" + badge.getName() + "'.");
			SCSettings.bcToggles.remove(playerUUID);
			return;
		}
		player.sendMessage(SCColor.Green + "Badge chat enabled for '" + badge.getName() + "'. Use /bc or /chat with no args to turn it off.");
	}
}
