package com.minetexas.suffixcommands.database;


import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import com.minetexas.suffixcommands.SuffixCommands;

public class BukkitObjects {

	private static SuffixCommands plugin = null;
	private static Server server = null;
	
	public static void initialize(SuffixCommands plugin) {

		BukkitObjects.plugin = plugin;
		BukkitObjects.server = plugin.getServer();
	}

	public static List<World> getWorlds() {
		return  getServer().getWorlds();
	}
	
	public static World getWorld(String name) {
		return  getServer().getWorld(name);
	}
	
	public static Server getServer() {
		synchronized(server) {
			return server;
		}
	}
	
	public static BukkitScheduler getScheduler() {
		return getServer().getScheduler();
	}
	
	public static int scheduleSyncDelayedTask(Runnable task, long delay) {
		return getScheduler().scheduleSyncDelayedTask(plugin, task, delay);
	}
	
	public static BukkitTask scheduleAsyncDelayedTask(Runnable task, long delay) {
		return getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
	}
	
	public static int scheduleSyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, repeat);
	}
	
	public static BukkitTask scheduleAsyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().runTaskTimerAsynchronously(plugin, task, delay, repeat);
	}

}
