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

/**
 * Query represents an SQL query. In JDBC view, a query contains at least a
 * PreparedStatement. It can also contains a ResultSet. Statement and ResultSet
 * are automatically closed when Query or Transaction is closed.
 * <p>
 * The most important behavior is to return a list of Pojo instead of a
 * ResultSet.
 * </p>
 * <p>
 * The example show how to use it.
 * 
 * <pre>
 * Transaction transaction = null;
 * try {
 *   // Obtain a new transaction from the ConnectionManager
 *   transaction = connectionManager.getNewTransaction(false,
 *                             javax.sql.Connection.TRANSACTION_READ_COMMITTED);
 *   // Start a new Query
 *   Query query = transaction.prepare(&quot;SELECT * FROM MyTable WHERE status=?&quot;);
 *   query.getStatement().setString(1, &quot;open&quot;);
 *   query.setFirstResult(0);
 *   query.setMaxResults(10);
 *   
 *   // Get the result
 *   List&lt;MyPojo&gt; myPojoList = query.getResultList(MyPojo.class));
 *   
 *   // do everything you need
 *   
 * } finally {
 *   // Release the transaction
 *   if (transaction != null)
 *     transaction.close();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author Emmanuel Keller
 * 
 */
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

	/**
	 * Set the first position of the result
	 * 
	 * @param firstResult
	 */
	public void setFirstResult(int firstResult) {
		this.firstResult = firstResult;
	}

	/**
	 * Set the maximum number of rows
	 * 
	 * @param maxResults
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * Close all component of that query (ResultSet and Statement)
	 */
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

	/**
	 * Get the PreparedStatement used by that Query
	 * 
	 * @return a PreparedStatement
	 */
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

	/**
	 * Returns the list of POJO. The list is cached. Every subsequent call
	 * returns the same list.
	 * 
	 * @param beanClass
	 *            The class name of POJO returned in the list
	 * @return a list of POJO
	 * @throws SQLException
	 */
	public List<? extends Object> getResultList(Class<?> beanClass)
			throws SQLException {
		if (resultList != null)
			return resultList;
		checkResultSet();
		resultList = createBeanList(beanClass);
		return resultList;
	}

	/**
	 * @return a list of Row object.
	 * @throws SQLException
	 */
	public List<Row> getResultList() throws SQLException {
		checkResultSet();
		return createRowList();
	}

	/**
	 * Do a PreparedStatement.executeUpdate(). A convenient way to execute an
	 * INSERT/UPDATE/DELETE SQL statement.
	 * 
	 * @return a row count
	 * @throws SQLException
	 */
	public int update() throws SQLException {
		return statement.executeUpdate();
	}

	/**
	 * FirstResult and MaxResults parameters are ignored.
	 * 
	 * @return the number of row found for a select
	 * @throws SQLException
	 */
	public int getResultCount() throws SQLException {
		checkResultSet();
		resultSet.last();
		return resultSet.getRow();
	}

	/**
	 * Get the ResultSet used by that Query.
	 * 
	 * @return the JDBC ResultSet
	 * @throws SQLException
	 */
	public ResultSet getResultSet() throws SQLException {
		checkResultSet();
		return resultSet;
	}
}
