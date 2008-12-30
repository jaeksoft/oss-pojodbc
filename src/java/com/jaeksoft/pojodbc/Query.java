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

import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jaeksoft.pojodbc.connection.ConnectionManager;

public class Query {

	private ResultSet resultSet;
	private List<?> resultList;
	private PreparedStatement statement;
	private int firstResult;
	private int maxResults;

	static protected Logger logger = Logger.getLogger(Query.class
			.getCanonicalName());

	protected Query(PreparedStatement statement) {
		this.statement = statement;
		firstResult = 0;
		maxResults = -1;
	}

	public void setFirstResult(int firstResult) {
		this.firstResult = firstResult;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	protected void close() {
		ConnectionManager.close(resultSet, statement, null);
	}

	private class MethodColumnIndex {
		private int columnIndex;
		private Method method;

		private MethodColumnIndex(int columnIndex, Method method) {
			this.columnIndex = columnIndex;
			this.method = method;
		}

		private void invoke(Object bean, ResultSet resultSet) throws Exception {
			if (method == null)
				return;
			Object colObject = resultSet.getObject(columnIndex);
			try {
				if (colObject != null)
					method.invoke(bean, colObject);
			} catch (Exception e) {
				if (method == null)
					throw new Exception("No method found for column "
							+ columnIndex, e);
				throw new Exception("Error on column "
						+ columnIndex
						+ " method "
						+ method.getName()
						+ (colObject == null ? "" : " object class is "
								+ colObject.getClass().getName()), e);
			}
		}
	}

	private List<?> createBeanList(Class<?> beanClass) throws SQLException {
		try {
			// Find related methods and columns
			ResultSetMetaData rs = resultSet.getMetaData();
			int columnCount = rs.getColumnCount();
			BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
			PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
			ArrayList<MethodColumnIndex> methods = new ArrayList<MethodColumnIndex>();

			if (logger.isLoggable(Level.FINEST))
				logger.finest("Search properties for bean "
						+ beanClass.getSimpleName());
			for (int i = 1; i <= columnCount; i++) {
				String columnName = rs.getColumnLabel(i);
				for (PropertyDescriptor propDesc : props) {
					if (propDesc.getWriteMethod() != null
							&& propDesc.getName().equalsIgnoreCase(columnName)) {
						methods.add(new MethodColumnIndex(i, propDesc
								.getWriteMethod()));
						if (logger.isLoggable(Level.FINEST))
							logger.finest("Found property \""
									+ propDesc.getName()
									+ "\" for column name \"" + columnName
									+ "\"");
						break;
					}
				}
			}
			// Create bean list
			ArrayList<Object> list = new ArrayList<Object>();
			moveToFirstResult();
			int limit = maxResults;
			while (resultSet.next() && limit-- != 0) {
				Object bean = Beans.instantiate(beanClass.getClassLoader(),
						beanClass.getCanonicalName());
				for (MethodColumnIndex methodColumnIndex : methods)
					methodColumnIndex.invoke(bean, resultSet);
				list.add(bean);
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void moveToFirstResult() throws SQLException {
		if (firstResult == 0)
			return;
		switch (statement.getResultSetType()) {
		case ResultSet.TYPE_FORWARD_ONLY:
			int i = firstResult;
			while (i-- > 0)
				resultSet.next();
			break;
		default:
			resultSet.absolute(firstResult);
			break;
		}
	}

	private List<Row> createRowList() throws SQLException {
		ResultSetMetaData rs = resultSet.getMetaData();
		int columnCount = rs.getColumnCount();
		ArrayList<Row> rows = new ArrayList<Row>();
		moveToFirstResult();
		int limit = maxResults;
		while (resultSet.next() && limit-- != 0)
			rows.add(new Row(columnCount, resultSet));
		return rows;
	}

	public PreparedStatement getStatement() {
		return statement;
	}

	private void checkResultSet() throws SQLException {
		if (resultSet != null)
			return;
		if (maxResults != -1)
			statement.setFetchSize(maxResults);
		resultSet = statement.executeQuery();
	}

	public List<? extends Object> getResultList(Class<?> beanClass)
			throws SQLException {
		if (resultList != null)
			return resultList;
		checkResultSet();
		resultList = createBeanList(beanClass);
		return resultList;
	}

	public List<Row> getResultList() throws SQLException {
		checkResultSet();
		return createRowList();
	}

	public int update() throws SQLException {
		return statement.executeUpdate();
	}

	public int getResultCount() throws SQLException {
		checkResultSet();
		resultSet.last();
		return resultSet.getRow();
	}

	public ResultSet getResultSet() throws SQLException {
		checkResultSet();
		return resultSet;
	}
}
