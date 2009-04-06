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

import java.sql.SQLException;

import javax.sql.DataSource;

import com.jaeksoft.pojodbc.Transaction;

/**
 * A connection manager getting database connection from a javax.sql.DataSource.
 * <p>
 * That example show how to create an instance of a DataSourceConnection using
 * DataSource from JNDI.
 * 
 * <pre>
 * Context initContext = new InitialContext();
 * Context envContext = (Context) initContext.lookup(&quot;java:/comp/env&quot;);
 * DataSource ds = (DataSource) envContext.lookup(&quot;myDatabase&quot;);
 * DatabaseConnectionManager connectionManager = new DataSourceConnection(ds);
 * </pre>
 * 
 * </p>
 * 
 * @author Emmanuel Keller
 * 
 */
public class DataSourceConnection extends ConnectionManager {

	private DataSource dataSource;

	/**
	 * @param dataSource
	 *            The DataSource that connection manager will use to get new
	 *            database connection.
	 */
	public DataSourceConnection(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Transaction getNewTransaction(boolean autoCommit,
			int transactionIsolation) throws SQLException {
		return new Transaction(dataSource.getConnection(), autoCommit,
				transactionIsolation);
	}

}
