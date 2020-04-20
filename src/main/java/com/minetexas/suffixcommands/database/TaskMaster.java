package com.minetexas.suffixcommands.database;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.degoos.wetsponge.task.WSTask;
import com.minetexas.suffixcommands.util.SCSettings;

public class TaskMaster {
	
	private static HashMap<String, WSTask> tasks = new HashMap<String, WSTask>();
	private static HashMap<String, WSTask> timers = new HashMap<String, WSTask>();
	
	
	public static long getTicksTilDate(Date date) {
		Calendar c = Calendar.getInstance();
		
		if (c.getTime().after(date)) {
			return 0;
		}
		
		long timeInSeconds = (date.getTime() - c.getTime().getTime() ) / 1000;
		return timeInSeconds*20;
	}
	
	public static long getTicksToNextHour() {
		Calendar c = Calendar.getInstance();
		Date now = c.getTime();
		
		c.add(Calendar.HOUR_OF_DAY, 1);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		
		Date nextHour = c.getTime();
		
		long timeInSeconds = (nextHour.getTime() - now.getTime())/1000;
		return timeInSeconds*20;
	}
	
	
	
	public static void syncTask(Runnable runnable) {
		WSTask task = WSTask.of(runnable);
		task.run(SCSettings.plugin);
	}
	
	public static void syncTask(Runnable runnable, long l) {
		WSTask task = WSTask.of(runnable);
		task.runTaskLater(1, SCSettings.plugin);
	}

	public static void asyncTimer(String name, Runnable runnable, long delay, long repeat) {
		WSTask task = WSTask.of(runnable);
		task.runTaskTimerAsynchronously(delay, delay, repeat, SCSettings.plugin);
		addTimer(name, task);
	}
	
	public static void asyncTimer(String name, Runnable runnable, long time) {
		WSTask task = WSTask.of(runnable);
		task.runTaskTimerAsynchronously(time, time, SCSettings.plugin);
		addTimer(name, task);
	}
	
	public static void asyncTask(String name, Runnable runnable, long delay) {
		WSTask task = WSTask.of(runnable);
		addTask(name, task);
		task.runTaskLaterAsynchronously(delay, SCSettings.plugin);
	}
	
	public static void asyncTask(Runnable runnable, long delay) {
		WSTask task = WSTask.of(runnable);
		task.runTaskLaterAsynchronously(delay, SCSettings.plugin);
	}
	
	private static void addTimer(String name, WSTask timer) {
		timers.put(name, timer);
	}
	
	private static void addTask(String name, WSTask task) {
		//RJ.out("Added task:"+name);
		tasks.put(name, task);
	}
	
	public static void stopAll() {
		stopAllTasks();
		stopAllTimers();
	}
	
	public static void stopAllTasks() {
		for (WSTask task : tasks.values()) {
			task.cancel();
		}
		tasks.clear();		
	}
	
	public static void stopAllTimers() {
		for (WSTask timer : timers.values()) {
			timer.cancel();
		}
		//RJ.out("clearing timers");

		timers.clear();
	}

	public static void cancelTask(String name) {
		WSTask task = tasks.get(name);
		if (task != null) {
			task.cancel();
		}
		//RJ.out("clearing tasks");

		tasks.remove(name);
	}
	
	public static void cancelTimer(String name) {
		WSTask timer = tasks.get(name);
		if (timer != null) {
			timer.cancel();
		}
		//RJ.out("cancel timer:"+name);

		timers.remove(name);
	}

	public static WSTask getTimer(String name) {
		return timers.get(name);
	}
	
	public static WSTask getTask(String name) {
		return tasks.get(name);
	}


//	public static void syncTimer(String name, Runnable runnable, long time) {
//		WSTask task = WSTask.of(runnable);
//		task.runTaskTimer(delay, SCSettings.plugin);
//		BukkitObjects.scheduleSyncRepeatingTask(runnable, time, time);
//	}
//
//	public static void syncTimer(String name, Runnable runnable, long delay, long repeat) {
//		BukkitObjects.scheduleSyncRepeatingTask(runnable, delay, repeat);
//		
//	}

//	public static boolean hasTask(String key) {
//		WSTask task = tasks.get(key);
//		
//		if (task == null) {
//			return false;
//		}
//		if (rask.isRunning())
//		
//		if (BukkitObjects.getScheduler().isCurrentlyRunning(task.getUniqueId()) || BukkitObjects.getScheduler().isQueued(task.getUniqueId())) {
//			return true;
//		} 
//		
//		tasks.remove(key);
//				
//		return false;
//	}

}
