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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jaeksoft.pojodbc.Transaction;

/**
 * 
 * The abstract class for all ConnectionManager.
 * 
 * @author Jaeksoft / Emmanuel Keller
 * 
 */
public abstract class ConnectionManager {

	/**
	 * Use com.jaeksoft.pojodbc.connection.ConnectionManager to manager the log
	 * level.
	 */
	static protected Logger logger = Logger.getLogger(ConnectionManager.class
			.getCanonicalName());

	/**
	 * Start a new transaction (or/and a new connection).
	 * 
	 * @param autoCommit
	 *            Enable or disable autocommit (if available)
	 * @param transactionIsolation
	 *            java.sql.Connection.TRANSACTION...
	 * @return a new Transaction object
	 * @throws SQLException
	 */
	public abstract Transaction getNewTransaction(boolean autoCommit,
			int transactionIsolation) throws SQLException;

	/**
	 * Start a new transaction (or/and a new connection) with autoCommit set to
	 * true, and transactionIsolation set to
	 * Connection.TRANSACTION_READ_UNCOMMITTED
	 * 
	 * @return a new Transaction object
	 * @throws SQLException
	 */
	public Transaction getNewTransaction() throws SQLException {
		return getNewTransaction(true, Connection.TRANSACTION_READ_UNCOMMITTED);
	}

	/**
	 * That static method try to close quietly each parameters. Null parameters
	 * are allowed. SQLException are catched and logged.
	 * 
	 * @param resultSet
	 *            A ResultSet to close
	 * @param stmt
	 *            A Statement to close
	 * @param cnx
	 *            A connection to close
	 */
	public static void close(ResultSet resultSet, Statement stmt, Connection cnx) {
		if (resultSet != null)
			try {
				resultSet.close();
			} catch (SQLException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		if (stmt != null)
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		if (cnx != null) {
			try {
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Close JDBC connection");
				cnx.close();
			} catch (SQLException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}
