package com.minetexas.suffixcommands.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnectionPool {

	HikariDataSource pool;
	
	public static void loadClass(String name) {
		//File file = new File("CivCraft/lib");
		
	}
	
	public static void init() throws ClassNotFoundException {
		/* Load any dependent classes. */
		
		/* load the database driver */
        Class.forName("com.mysql.jdbc.Driver");
	}
	
	
	public ConnectionPool(String dbcUrl, String user, String pass, int minConns,
			int maxConns, int partCount) throws ClassNotFoundException, SQLException {
		/*
		 * Initialize our connection pool.
		 * 
		 * We'll use a connection pool and reuse connections on a per-thread basis. 
		 */
				
		/* setup the connection pool */
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbcUrl); 
		config.setUsername(user); 
		config.setPassword(pass);
		// Enable only for debugging.
		//config.setCloseConnectionWatch(true);
		
		pool = new HikariDataSource(config);
	}
	
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}
	
	public void shutdown() {
		pool.close();
	}
}

