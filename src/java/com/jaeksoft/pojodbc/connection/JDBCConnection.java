/**   
 * License Agreement for Jaeksoft Pojodbc
 *
 * Copyright (C) 2008 Emmanuel Keller / Jaeksoft
 * 
 * http://www.jaeksoft.com
 * 
 * This file is part of Jaeksoft Pojodbc.
 *
 * Jaeksoft Pojodbc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft Pojodbc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft Pojodbc.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.pojodbc.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.jaeksoft.pojodbc.Transaction;

/**
 * A connection manager getting database connection from an JDBC url.
 * <p>
 * Take care to set driver and url parameters before using getNewTransaction
 * method.
 * </p>
 * <p>
 * That example show how create an instance of a JDBCConnection using a MySQL
 * database.
 * 
 * <pre>
 * JDBCConnection connectionManager = new JDBCConnection();
 * connectionManager.setDriver(&quot;com.mysql.jdbc.Driver&quot;);
 * connectionManager
 * 		.setUrl(&quot;jdbc:mysql://localhost:3306/dbName?autoReconnect=true&quot;);
 * </pre>
 * 
 * </p>
 * 
 * @author Emmanuel Keller
 * 
 */
public class JDBCConnection extends ConnectionManager {

	private String url;

	private String driver;

	private String username;

	private String password;

	/**
	 * The empty constructor. Used for bean compatibility. Parameters can be
	 * passed using setters.
	 */
	public JDBCConnection() {
		url = null;
		driver = null;
	}

	/**
	 * 
	 * @param driver
	 *            The driver class name
	 * @param url
	 *            The url used to connect to database
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public JDBCConnection(String driver, String url)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		setDriver(driver);
		setUrl(url);
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		if (logger.isLoggable(Level.FINEST))
			logger.finest("New Database instance - Driver: " + driver
					+ " Url: " + url);
		if (driver != null)
			Class.forName(driver).newInstance();
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public Transaction getNewTransaction(boolean autoCommit,
			int transactionIsolation) throws SQLException {
		return getNewTransaction(autoCommit, transactionIsolation, null);
	}

	/**
	 * Get a new Transaction instance. You can add a suffix on the url used to
	 * establish the database connection.
	 * 
	 * @param autoCommit
	 * @param transactionIsolation
	 * @param urlSuffix
	 *            A suffix added to the url when establishing the database
	 *            connection
	 * @return a new Transaction instance
	 * @throws SQLException
	 */
	public Transaction getNewTransaction(boolean autoCommit,
			int transactionIsolation, String urlSuffix) throws SQLException {
		String localUrl = url;
		if (urlSuffix != null)
			localUrl += urlSuffix;
		if (logger.isLoggable(Level.FINEST))
			logger.finest("DriverManager.getConnection " + localUrl);
		Connection cnx = null;
		if (username != null || password != null)
			cnx = DriverManager.getConnection(localUrl, username, password);
		else
			cnx = DriverManager.getConnection(localUrl);
		cnx.setTransactionIsolation(transactionIsolation);
		cnx.setAutoCommit(autoCommit);
		return new Transaction(cnx, autoCommit, transactionIsolation);
	}
}
