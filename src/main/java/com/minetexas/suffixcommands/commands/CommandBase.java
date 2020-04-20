package com.minetexas.suffixcommands.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.minetexas.suffixcommands.exception.SCException;
import com.minetexas.suffixcommands.util.SCColor;

import com.degoos.wetsponge.command.WSCommand;
import com.degoos.wetsponge.command.WSCommandSource;
import com.degoos.wetsponge.entity.living.player.WSPlayer;
import com.degoos.wetsponge.text.WSText;


public abstract class CommandBase extends WSCommand {
	
	public CommandBase(String name, String description) {
		super(name, description);
	}
	protected HashMap<String, String> commands = new HashMap<String, String>();
	protected String[] args;
	protected WSCommandSource sender;
	
	public abstract void init();
	protected String command = "FIXME";
	protected String displayName = "FIXME";
	protected boolean sendUnknownToDefault = false;
	
	/* Called when no arguments are passed. */
	public abstract void doDefaultAction() throws SCException;
	
	/* Called on syntax error. */
	public abstract void showHelp();
	
	/* Called before command is executed to check permissions. */
	public abstract void permissionCheck()  throws SCException;
	
	@Override
    public void executeCommand(WSCommandSource sender, String cmd, String[] args) {
		init();
		this.args = args;
		this.sender = sender;
		
		try {
			permissionCheck();
		} catch (SCException e1) {
			sendError(sender, e1.getMessage());
			return;
		}
		
		if (args.length == 0) {
			try {
				doDefaultAction();
			} catch (SCException e) {
				sendError(sender, e.getMessage());
			}
			return;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return;
		}
		
		for (String c : commands.keySet()) {
			  if (c.equalsIgnoreCase(args[0])) {
					try { 
						Method method = this.getClass().getMethod(args[0].toLowerCase()+"_cmd");
						try {
							method.invoke(this);
							return;
						} catch (IllegalAccessException | IllegalArgumentException e) {
							e.printStackTrace();
							sendError(sender, "Internal Command Exception");
						} catch (InvocationTargetException e) {
							if (e.getCause() instanceof SCException) {
								sendError(sender, e.getCause().getMessage());
							} else {
								sendError(sender, "Internal Command Exception");
								e.getCause().printStackTrace();
							}
						}

						
					} catch (NoSuchMethodException e) {
						if (sendUnknownToDefault) {
							try {
								doDefaultAction();
							} catch (SCException e1) {
								sendError(sender, e.getMessage());
							}
							return;
						}
						sendError(sender, "Unkown Command: "+args[0]);
					}
					return;
				}
			}
			
			if (sendUnknownToDefault) {
				try {
					doDefaultAction();
				} catch (SCException e) {
					sendError(sender, e.getMessage());
				}
				return;
			}
			
			sendError(sender, "Unkown Command: "+args[0]);
			return;
	}
	
	public WSPlayer getPlayer() throws SCException {
		if (sender instanceof WSPlayer) {
			return (WSPlayer)sender;
		}
		throw new SCException("You must be a player to execute this command");
	}
	
	public static void sendMessage(Object sender, WSText line) {
		if ((sender instanceof WSPlayer)) {
//			SCLog.debug(((Player) sender).getDisplayName()+" - "+line);
			((WSPlayer) sender).sendMessage(line);
		} else if (sender instanceof WSCommandSource) {
			((WSCommandSource) sender).sendMessage(line);
		}
	}
	
	public static void sendMessage(Object sender, String line) {
		if ((sender instanceof WSPlayer)) {
//			SCLog.debug(((Player) sender).getDisplayName()+" - "+line);
			((WSPlayer) sender).sendMessage(line);
		} else if (sender instanceof WSCommandSource) {
			((WSCommandSource) sender).sendMessage(line);
		}
	}
	public static void sendMessage(Object sender, String[] lines) {
		boolean isPlayer = false;
		if (sender instanceof WSPlayer)
			isPlayer = true;

		for (String line : lines) {
			if (isPlayer) {
				((WSPlayer) sender).sendMessage(line);
			} else {
				((WSCommandSource) sender).sendMessage(line);
			}
		}
	}
	
	public static String buildTitle(String title) {
		String line =   "-------------------------------------------------";
		String titleBracket = "[ " + SCColor.Yellow + title + SCColor.LightBlue + " ]";
		
		if (titleBracket.length() > line.length()) {
			return SCColor.LightBlue+"-"+titleBracket+"-";
		}
		
		int min = (line.length() / 2) - titleBracket.length() / 2;
		int max = (line.length() / 2) + titleBracket.length() / 2;
		
		String out = SCColor.LightBlue + line.substring(0, Math.max(0, min));
		out += titleBracket + line.substring(max);
		
		return out;
	}
	
	public static void sendHeading(WSCommandSource sender, String title) {	
		sendMessage(sender, buildTitle(title));
	}
	
	public static void sendError(Object sender, String line) {		
		sendMessage(sender, SCColor.Rose+line);
	}


}
