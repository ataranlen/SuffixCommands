package com.minetexas.suffixcommands.database;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitTask;

import com.minetexas.suffixcommands.SuffixCommands;
import com.minetexas.suffixcommands.util.SCLog;

public class SQLUpdate implements Runnable {

	public static final int QUEUE_SIZE = 4096;
	private static final int UPDATES_PER_RUN = 50;
	private static final Object LOCK = new Object();
	private static final Map<SQLObject, Boolean> saveObjects =
			new LinkedHashMap<SQLObject, Boolean>();
	private static BukkitTask workerTask;

	public static ConcurrentHashMap<String, Integer> statSaveRequests =
			new ConcurrentHashMap<String, Integer>();
	public static ConcurrentHashMap<String, Integer> statSaveCompletions =
			new ConcurrentHashMap<String, Integer>();

	public static void start(SuffixCommands plugin) {
		synchronized (LOCK) {
			if (workerTask == null) {
				workerTask = plugin.getServer().getScheduler()
						.runTaskTimerAsynchronously(plugin, new SQLUpdate(), 1L, 10L);
			}
		}
	}

	public static void stopAndFlush() {
		synchronized (LOCK) {
			if (workerTask != null) {
				workerTask.cancel();
				workerTask = null;
			}
			savePending(Integer.MAX_VALUE);
		}
	}

	public static void add(SQLObject obj) {
		if (obj == null) {
			return;
		}

		synchronized (LOCK) {
			increment(statSaveRequests, obj);
			if (!saveObjects.containsKey(obj)) {
				if (saveObjects.size() >= QUEUE_SIZE) {
					SCLog.warning("SQL update queue is full; dropping update for "
							+ obj.getClass().getSimpleName() + " '" + obj.getName() + "'");
					return;
				}
				saveObjects.put(obj, Boolean.TRUE);
			}
		}
	}

	public static void save() {
		synchronized (LOCK) {
			savePending(Integer.MAX_VALUE);
		}
	}

	private static void savePending(int limit) {
		Iterator<SQLObject> iterator = saveObjects.keySet().iterator();
		int saved = 0;
		while (iterator.hasNext() && saved < limit) {
			SQLObject obj = iterator.next();
			iterator.remove();
			try {
				obj.saveNow();
				increment(statSaveCompletions, obj);
			} catch (SQLException e) {
				SCLog.exception("Failed to save queued SQL object", e);
			}
			saved++;
		}
	}

	private static void increment(ConcurrentHashMap<String, Integer> stats, SQLObject obj) {
		String type = obj.getClass().getSimpleName();
		stats.merge(type, 1, Integer::sum);
	}

	@Override
	public void run() {
		synchronized (LOCK) {
			savePending(UPDATES_PER_RUN);
		}
	}
}
