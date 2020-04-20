package com.minetexas.suffixcommands.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.degoos.wetsponge.WetSponge;
import com.degoos.wetsponge.command.WSCommandSource;
import com.degoos.wetsponge.entity.living.player.WSPlayer;
import com.degoos.wetsponge.text.WSText;
import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.ConfigBadges;
import com.minetexas.suffixcommands.util.SCColor;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class ChatCommand extends CommandBase {

	public ChatCommand() {
		super("chat", "Chat with your Badge Groups");
	}

	@Override
	public List<String> sendTab(WSCommandSource arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		List<String> keys = new ArrayList<>(commands.keySet());
		return keys;
	}

	@Override
	public void init() {
		command = "/chat";
		displayName = "Chat with your Badge Groups";
		
		commands.put("list", "List all badge chat channels you can access");
	}
	
	@Override
	public void executeCommand (WSCommandSource sender, String cmd, String[] args) {
		init();
		
		this.args = args;
		this.sender = sender;
		
		permissionCheck();
		
		if (args.length == 0) {
			doDefaultAction();
			return;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return;
		}
		

		try {
			parse_chat();
			return;
		} catch (SCException e) {
			sendError(sender, e.getMessage());
			return;
		}
	}
	
	public void parse_chat() throws SCException {		
		if (args.length < 1) {
			throw new SCException("Enter a Badge Name");
		}
		
		if (args[0].equalsIgnoreCase("list"))
		{
			list_cmd();
			return;
		}
		
		if (args.length < 2) {
			throw new SCException("You have to say something.");
		}
		Badge badge = SCSettings.badges.get(args[0]);
		if (badge == null) {
			ConfigBadges legacyBadge = SCSettings.legacyBadges.get(args[0]);
			if (legacyBadge == null)
			{
				throw new SCException("Invalid Badge Name. Use exact spelling and capitalization");
			}
			
			if (permissionCheck(SCSettings.PERMISSION_CHAT+legacyBadge.name)) {
				
				String mainColor = legacyBadge.chatColor;
				StringBuilder builder = new StringBuilder();
				builder.append(mainColor+"["+sender.getName()+legacyBadge.badgeText+mainColor+"]"+mainColor);
				
				args[0] = "";
				
				for(String s : args) {
					if (s.length() >= 1) {
						builder.append(" "+s);
					}
				}
				
				String message = builder.toString();
				SCLog.info(message);

				Collection <? extends WSPlayer> players =  WetSponge.getServer().getOnlinePlayers();

				for(WSPlayer p : players) {
					if (p.hasPermission(SCSettings.PERMISSION_CHAT+legacyBadge.name)) {
						p.sendMessage(WSText.of(message).toBuilder().translateColors().build());	
					}
				}
				
				
				
				
			} else {
				sendMessage(sender, SCColor.Red+"You don't have access to the '"+legacyBadge.name+"' Badge Group.");
			}
		} else {
			WSPlayer player = getPlayer();
			String playerUUID = player.getUniqueId().toString();
			if (badge.canUseBadge(playerUUID)) {			
				String mainColor = badge.getChatColor();
				StringBuilder builder = new StringBuilder();
				SCLog.info(mainColor + "Test");
				builder.append(mainColor+"["+sender.getName()+badge.getBadgeText()+mainColor+"]"+mainColor);
				args[0] = "";

				SCLog.info(builder.toString());
				for(String s : args) {
					if (s.length() >= 1) {
						builder.append(" "+s);
					}
				}
				
				String message = builder.toString();
				SCLog.info(message);

				Collection <? extends WSPlayer> players = WetSponge.getServer().getOnlinePlayers();

				for(WSPlayer p : players) {
					if (badge.canUseBadge(p.getUniqueId().toString())) {
						p.sendMessage(WSText.of(message).toBuilder().translateColors().build());	
					}
				}
			} else {
				sendMessage(sender, SCColor.Red+"You don't have access to the '"+badge.getName()+"' Badge Group.");
			}
		}
	}
	
	public void list_cmd() throws SCException {
		Boolean hasBadges = false;
		sendHeading(sender, "List Badge Chat Channels");
		WSPlayer sender = getPlayer();
		String senderUUID = sender.getUniqueId().toString();
		for (Badge badge : SCSettings.badges.values()) {
			if (badge.canUseBadge(senderUUID)) {
				String status = "";
				if (badge.isLeader(senderUUID)) {
					status = "Leader";
				} else if (badge.isMember(senderUUID)) {
					status = "Member";
				} else {
					status = "Owner";
				}
				sender.sendMessage(WSText.of(badge.getName()+SCColor.Green+" ["+status+"]:"+badge.getChatColor()).toBuilder().translateColors().build());
				hasBadges = true;
			}
		}
		if (!hasBadges) {
			sendMessage(sender, "You don't own any Group badges");
		}
	}

	@Override
	public void doDefaultAction() {		
		if (args.length >= 1) {
			try {
				parse_chat();
			} catch (SCException e) {
				sendError(sender, e.getCause().getMessage());
//				e.printStackTrace();
			}
		} else {
			showBasicHelp();
		}
	}

	@Override
	public void showHelp() {
		WSPlayer player;
		try {
			player = getPlayer();
		} catch (SCException e) {
			e.printStackTrace();
			return;
		}
		
		if (player == null || !player.hasPermission(SCSettings.BADGE)) {
			return;
		}
		
		showBasicHelp();

	}
	
	public void showBasicHelp() {
		sendHeading(sender, displayName);
		for (String c : commands.keySet()) {
			String info = commands.get(c);
			
			info = info.replace("[", SCColor.Yellow+"[");
			info = info.replace("]", "]"+SCColor.LightGray);
			info = info.replace("(", SCColor.Yellow+"(");
			info = info.replace(")", ")"+SCColor.LightGray);
						
			sendMessage(sender, SCColor.LightPurple+command+" "+c+SCColor.LightGray+" "+info);
		}
	}
	
	public Boolean permissionCheck(String permission) {
		WSPlayer player;
		try {
			player = getPlayer();
		} catch (SCException e) {
			e.printStackTrace();
			return false;
		}
		
		if (!player.hasPermission(permission)) {
			return false;
		}
		return true;
	}

	@Override
	public void permissionCheck() {
		// TODO Auto-generated method stub

	}

}
