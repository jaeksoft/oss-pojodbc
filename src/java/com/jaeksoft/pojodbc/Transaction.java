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
import com.jaeksoft.pojodbc.Query;

public class Transaction {

	private Connection cnx;
	private HashSet<Query> queries;

	public Transaction(Connection cnx, boolean autoCommit,
			int transactionIsolation) throws SQLException {
		this.cnx = cnx;
		cnx.setTransactionIsolation(transactionIsolation);
		cnx.setAutoCommit(autoCommit);
	}

	public void closeQuery(Query query) {
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

	public void rollback() throws SQLException {
		synchronized (cnx) {
			cnx.rollback();
		}
	}

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

	public Query prepare(String sql) throws SQLException {
		Query query = new Query(cnx.prepareStatement(sql));
		addQuery(query);
		return query;
	}

	public Query prepare(String sql, int resultSetType, int resultSetConcurency)
			throws SQLException {
		Query query = new Query(cnx.prepareStatement(sql, resultSetType,
				resultSetConcurency));
		addQuery(query);
		return query;
	}

	public int update(String sql) throws SQLException {
		return prepare(sql).update();
	}
}
