package com.minetexas.suffixcommands.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.degoos.wetsponge.plugin.WSPlugin;
import com.minetexas.suffixcommands.exception.SCException;

public class SCLog {

	public static WSPlugin plugin;
	private static Logger cleanupLogger;
	
	public static void init(WSPlugin plugin) {
		SCLog.plugin = plugin;
		
		cleanupLogger = Logger.getLogger("cleanUp");
		FileHandler fh;
		
		try {
			fh = new FileHandler("cleanUp.log");
			cleanupLogger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void heading(String title) {
		plugin.getLogger().sendInfo("========= "+title+" =========");
	}
	
	public static void info(String message) {
		plugin.getLogger().sendInfo(message);
	}
	
	public static void debug(String message) {
		plugin.getLogger().sendInfo("[DEBUG] "+message);
	}

	public static void warning(String message) {
		if (message == null) {
			try {
				throw new SCException("Null warning message!");
			} catch (SCException e){
				e.printStackTrace();
			}
		}
		plugin.getLogger().sendWarning("[WARNING] "+message);
	}

	public static void error(String message) {
		plugin.getLogger().sendError(message);
	}
	
	public static void adminlog(String name, String message) {
		plugin.getLogger().sendInfo("[ADMIN:"+name+"] "+message);
	}
	
	public static void cleanupLog(String message) {
		info(message);
		cleanupLogger.info(message);		
	}
	
	public static void exception(String string, Exception e) {
		e.printStackTrace();		
	}
}
