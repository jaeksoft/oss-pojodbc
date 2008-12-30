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
import com.jaeksoft.pojodbc.connection.ConnectionManager;

public class DataSourceConnection extends ConnectionManager {

	private DataSource dataSource;

	public DataSourceConnection(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Transaction getNewTransaction(boolean autoCommit,
			int transactionIsolation) throws SQLException {
		return new Transaction(dataSource.getConnection(), autoCommit,
				transactionIsolation);
	}

}
