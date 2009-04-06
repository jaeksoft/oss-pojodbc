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

package com.jaeksoft.pojodbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;

import com.jaeksoft.pojodbc.connection.ConnectionManager;

/**
 * Represents a new database transaction. Currently, a transaction represents a
 * database connection. Further implementation could share same connection.</p>
 * <p>
 * Transaction automatically closed every Query used.
 * </p>
 * <p>
 * That source code is our recommended way to use it. You have close the
 * transaction in a finally statement to be sure that the database connection
 * will be released.
 * 
 * <pre>
 * Transaction transaction = null;
 * try {
 * 	transaction = connectionManager.getNewTransaction(false,
 * 			javax.sql.Connection.TRANSACTION_READ_COMMITTED);
 * 	// ... do everything you need ...
 * } finally {
 * 	if (transaction != null)
 * 		transaction.close();
 * }
 * 
 * </pre>
 * 
 * @author ekeller
 * 
 */
public class Transaction {

	private Connection cnx;
	private HashSet<Query> queries;

	public Transaction(Connection cnx, boolean autoCommit,
			int transactionIsolation) throws SQLException {
		this.cnx = cnx;
		cnx.setTransactionIsolation(transactionIsolation);
		cnx.setAutoCommit(autoCommit);
	}

	public Transaction(Connection cnx) throws SQLException {
		this(cnx, true, Connection.TRANSACTION_NONE);
	}

	void closeQuery(Query query) {
		synchronized (this) {
			query.close();
			queries.remove(query);
		}
	}

	private void closeQueries() {
		synchronized (this) {
			if (queries == null)
				return;
			for (Query query : queries)
				query.close();
			queries.clear();
		}
	}

	/**
	 * Close all queries and the transaction. No commit or rollback are
	 * performed.
	 */
	public void close() {
		synchronized (this) {
			if (cnx == null)
				return;
			synchronized (cnx) {
				closeQueries();
				ConnectionManager.close(null, null, cnx);
				cnx = null;
			}
		}
	}

	/**
	 * Usual JDBC/SQL transaction rollback
	 * 
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		synchronized (cnx) {
			cnx.rollback();
		}
	}

	/**
	 * Usual JDBC/SQL transaction commit
	 * 
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		synchronized (cnx) {
			cnx.commit();
		}
	}

	private void addQuery(Query query) {
		synchronized (this) {
			if (queries == null)
				queries = new HashSet<Query>();
			queries.add(query);
		}
	}

	/**
	 * Create a new Query
	 * 
	 * @param sql
	 *            The native SQL query
	 * @return a new Query instance
	 * @throws SQLException
	 */
	public Query prepare(String sql) throws SQLException {
		Query query = new Query(cnx.prepareStatement(sql));
		addQuery(query);
		return query;
	}

	/**
	 * Create a new Query with standard JDBC properties.
	 * <p>
	 * ResultSetType and ResultSetConcureny are JDBC standard parameters like
	 * ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_FORWARD_ONLY,
	 * ResultSet.CONCUR_READ_ONLY, ResultSet.CONCUR_UPDATABLE, ...
	 * </p>
	 * 
	 * @param sql
	 *            The native SQL query
	 * @param resultSetType
	 *            A standard JDBC ResultSet type
	 * @param resultSetConcurency
	 *            A standard JDBC Result concurency property
	 * @return a new Query instance
	 * @throws SQLException
	 */
	public Query prepare(String sql, int resultSetType, int resultSetConcurency)
			throws SQLException {
		Query query = new Query(cnx.prepareStatement(sql, resultSetType,
				resultSetConcurency));
		addQuery(query);
		return query;
	}

	/**
	 * A convenient way to directly execute an INSERT/UPDATE/DELETE SQL
	 * statement.
	 * 
	 * @param sql
	 *            The native SQL query
	 * @return the row count
	 * @throws SQLException
	 */
	public int update(String sql) throws SQLException {
		return prepare(sql).update();
	}
}
