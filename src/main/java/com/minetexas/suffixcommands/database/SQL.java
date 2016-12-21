package com.minetexas.suffixcommands.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.minetexas.suffixcommands.Badge;
import com.minetexas.suffixcommands.exception.InvalidConfiguration;
import com.minetexas.suffixcommands.util.SCLog;
import com.minetexas.suffixcommands.util.SCSettings;

//import com.jolbox.bonecp.Statistics;

public class SQL {
	
	public static String hostname = "";
	public static String port = "";
	public static String db_name = "";
	public static String username = "";
	public static String password = "";
	public static String tb_prefix = "";
	
	private static String dsn = "";
	
	public static Integer min_conns;
	public static Integer max_conns;
	public static Integer parts;
	
	public static ConnectionPool badgeDatabase;

	public static void initialize() throws InvalidConfiguration, SQLException, ClassNotFoundException {
		SCLog.heading("Initializing SQL");
		
		SQL.hostname = SCSettings.getStringBase("mysql.hostname");
		SQL.port = SCSettings.getStringBase("mysql.port");
		SQL.db_name = SCSettings.getStringBase("mysql.database");
		SQL.username = SCSettings.getStringBase("mysql.username");
		SQL.password = SCSettings.getStringBase("mysql.password");
		SQL.tb_prefix = SCSettings.getStringBase("mysql.table_prefix");
		SQL.dsn = "jdbc:mysql://" + hostname + ":" + port + "/" + tb_prefix+db_name;
		SQL.min_conns = Integer.valueOf(SCSettings.getStringBase("mysql.min_conns"));
		SQL.max_conns = Integer.valueOf(SCSettings.getStringBase("mysql.max_conns"));
		SQL.parts = Integer.valueOf(SCSettings.getStringBase("mysql.parts"));

				SCLog.info("\t Using "+SQL.hostname+":"+SQL.port+" user:"+SQL.username+" DB:"+SQL.db_name);

		SCLog.info("\t Building Connection Pool for Badge database.");
		badgeDatabase = new ConnectionPool(SQL.dsn, SQL.username, SQL.password, SQL.min_conns, SQL.max_conns, SQL.parts);
		SCLog.info("\t Connected to Badge database");

		
		SCLog.heading("Initializing SQL Finished");
	}


	public static void initBadgeObjectTables() throws SQLException {	
		SCLog.heading("Building Badge Object Tables.");
		Badge.init();	
		SCLog.info("----- Done Building Tables ----");
	}
	
	
	public static Connection getGameConnection() throws SQLException {

		return badgeDatabase.getConnection();
	}
	
	public static boolean hasTable(String name) throws SQLException {
		Connection context = null;
		ResultSet result = null;
		try {
			context = getGameConnection();
			DatabaseMetaData dbm = context.getMetaData();
			String[] types = { "TABLE" };
			
			result = dbm.getTables(null, null, SQL.tb_prefix + name, types);
			if (result.next()) {
				return true;
			}
			return false;
		} finally {
			SQL.close(result, null, context);
		}
	}

	public static boolean hasColumn(String tablename, String columnName) throws SQLException {
		Connection context = null;
		ResultSet result = null;
		
		try {
			context = getGameConnection();		
			DatabaseMetaData dbm = context.getMetaData();
			result = dbm.getColumns(null, null, SQL.tb_prefix + tablename, columnName);
	    	boolean found = result.next();
	    	return found;
		} finally {
			SQL.close(result, null, context);
		}
	}
	
	public static void addColumn(String tablename, String columnDef) throws SQLException {		
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			String table_alter = "ALTER TABLE "+ SQL.tb_prefix + tablename +" ADD " +columnDef;
			context = getGameConnection();		
			ps = context.prepareStatement(table_alter);
			ps.execute();
			SCLog.info("\tADDED:"+columnDef);
		} finally {
			SQL.close(null, ps, context);
		}
		
	}
	
	public static void update(int id, HashMap<String, Object> hashmap, String tablename) throws SQLException {
		hashmap.put("id", id);
		update(hashmap, "id", tablename);
	}

	public static void update(HashMap<String,Object> hashmap, String keyname, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			String sql = "UPDATE `" + SQL.tb_prefix + tablename + "` SET ";
			String where = " WHERE `"+keyname+"` = ?;";
			ArrayList<Object> values = new ArrayList<Object>();
	
			Object keyValue = hashmap.get(keyname);
			hashmap.remove(keyname);
			
			Iterator<String> keyIter = hashmap.keySet().iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				
				sql += "`"+key+"` = ?";
				sql += "" + (keyIter.hasNext() ? ", " : " ");
				values.add(hashmap.get(key));
			}
			
			sql += where;
			
			context = SQL.getGameConnection();		
			ps = context.prepareStatement(sql);
					
			int i = 1;
			for (Object value : values) {
				if (value instanceof String) {
					ps.setString(i, (String) value);
				} else if (value instanceof Integer) {
					ps.setInt(i, (Integer)value);
				} else if (value instanceof Boolean) {
					ps.setBoolean(i, (Boolean)value);
				} else if (value instanceof Double) {
					ps.setDouble(i, (Double)value);
				} else if (value instanceof Float) {
					ps.setFloat(i, (Float)value);
				} else if (value instanceof Long) {
					ps.setLong(i, (Long)value);
				} else {
					ps.setObject(i, value);
				}
				i++;
			}
			
			ps.setObject(i, keyValue);
	
			if (ps.executeUpdate() == 0) {
				insertNow(hashmap, tablename);
			}
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void insert(HashMap<String, Object> hashmap, String tablename) {
		TaskMaster.asyncTask(new SQLInsertTask(hashmap, tablename), 0);
	}
	
	public static int insertNow(HashMap<String, Object> hashmap, String tablename) throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			String sql = "INSERT INTO " + SQL.tb_prefix + tablename + " ";
			String keycodes = "(";
			String valuecodes = " VALUES ( ";
			ArrayList<Object> values = new ArrayList<Object>();
			
			Iterator<String> keyIter = hashmap.keySet().iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				
				keycodes += key;
				keycodes += "" + (keyIter.hasNext() ? "," : ")");
				
				valuecodes += "?";
				valuecodes += "" + (keyIter.hasNext() ? "," : ")");
				
				values.add(hashmap.get(key));
			}
			
			sql += keycodes;
			sql += valuecodes;
			
			context = SQL.getGameConnection();		
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			
			int i = 1;
			for (Object value : values) {
				if (value instanceof String) {
					ps.setString(i, (String) value);
				} else if (value instanceof Integer) {
					ps.setInt(i, (Integer)value);
				} else if (value instanceof Boolean) {
					ps.setBoolean(i, (Boolean)value);
				} else if (value instanceof Double) {
					ps.setDouble(i, (Double)value);
				} else if (value instanceof Float) {
					ps.setFloat(i, (Float)value);
				} else if (value instanceof Long) {
					ps.setLong(i, (Long)value);
				} else {
					ps.setObject(i, value);
				}
				i++;
			}
			
			ps.execute();
			int id = 0;
			rs = ps.getGeneratedKeys();
	
			while (rs.next()) {
				id = rs.getInt(1);
				break;
			}
				
			if (id == 0) {
				String name = (String)hashmap.get("name");
				if (name == null) {
					name = "Unknown";
				}
				
				SCLog.error("SQL ERROR: Saving an SQLObject returned a 0 ID! Name:"+name+" Table:"+tablename);
			}
			return id;

		} finally {
			SQL.close(rs, ps, context);
		}
	}


	public static void deleteNamedObject(SQLObject obj, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			String sql = "DELETE FROM " + SQL.tb_prefix + tablename + " WHERE `id` = ?";
			context = SQL.getGameConnection();		
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, obj.getId());
			ps.execute();
			ps.close();
			obj.setDeleted(true);
		} finally {
			SQL.close(null, ps, context);
		}	
	}
	
	public static void deleteByName(String name, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			String sql = "DELETE FROM " + SQL.tb_prefix + tablename + " WHERE `name` = ?";
			context = SQL.getGameConnection();		
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.execute();
			ps.close();
		} finally {
			SQL.close(null, ps, context);
		}
	}
	public static void makeCol(String colname, String type, String TABLE_NAME) throws SQLException {
		if (!SQL.hasColumn(TABLE_NAME, colname)) {
			SCLog.info("\tCouldn't find "+colname+" column for "+TABLE_NAME);
			SQL.addColumn(TABLE_NAME, "`"+colname+"` "+type);				
		}
	}
	
	public static void makeTable(String table_create) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement(table_create);
			ps.execute();
		} finally {
			SQL.close(null, ps, context);
		}

	}	
	
	public static void close(ResultSet rs, PreparedStatement ps, Connection context) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if (context != null) {
			try {
				context.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateNamedObjectAsync(NamedObject obj, HashMap<String, Object> hashmap, String tablename) throws SQLException {
		TaskMaster.asyncTask("", new SQLUpdateNamedObjectTask(obj, hashmap, tablename), 0);
	}

	public static void updateNamedObject(SQLObject obj, HashMap<String, Object> hashmap, String tablename) throws SQLException {
		if (obj.isDeleted()) {
			return;
		}
		
		if (obj.getId() == 0) {
			obj.setId(SQL.insertNow(hashmap, tablename));
		} else {
			SQL.update(obj.getId(), hashmap, tablename);
		}	
	}
	
}

