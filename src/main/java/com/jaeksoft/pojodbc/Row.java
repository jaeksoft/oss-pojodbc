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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a row from a ResultSet. A convienient way to retrieve data from
 * ResultSet if you don't want to use POJO.
 * 
 * @author ekeller
 * 
 */
public class Row {

	private Object[] columns;

	protected Row(int columnCount) {
		columns = new Object[columnCount];
	}

	protected Row(int columnCount, ResultSet rs) throws SQLException {
		this(columnCount);
		for (int columnIndex = 0; columnIndex < columnCount; columnIndex++)
			columns[columnIndex] = rs.getObject(columnIndex + 1);
	}

	public void set(int column, Object value) {
		columns[column] = value;
	}

	public Object get(int column) {
		Object col = columns[column];
		if (col == null)
			return null;
		return col;
	}
}
