package com.minetexas.suffixcommands;

import java.io.IOException;

import com.degoos.wetsponge.WetSponge;
import com.degoos.wetsponge.command.WSCommandManager;
import com.degoos.wetsponge.plugin.WSPlugin;
import com.minetexas.suffixcommands.commands.BadgeCommand;
import com.minetexas.suffixcommands.commands.ChatCommand;
import com.minetexas.suffixcommands.database.SQLUpdate;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

public class SuffixCommands extends WSPlugin {
    // This will be our main plugin class
    private static SuffixCommands instance;

    public static SuffixCommands getInstance() {
        return instance;
    }

	public static boolean isDisable = false;
	
	@Override
    public void onEnable() {
        instance = this;
		SCLog.init(this);
		SCLog.info("onEnable has been invoked!");
		
		try {
			SCSettings.init(this);
		} catch (IOException | InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        WSCommandManager cm = WetSponge.getCommandManager();
        cm.addCommand(new ChatCommand());
        cm.addCommand(new BadgeCommand());
	}
 
	@Override
	public void onDisable() {
		super.onDisable();
		SCLog.info("onDisable has been invoked!");
		isDisable = true;
		SQLUpdate.save();
	}
	
//	public boolean hasPlugin(String name) {
//		WSPluginManager manager = WSPluginManager.getInstance(); 
//		Optional<WSPlugin> p;
//		p = manager.getPlugin(name);
//		return (p != null);
//	}
}
