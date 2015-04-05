/**   
 * License Agreement for OpenSearchServer Pojodbc
 *
 * Copyright 2008-2013 Emmanuel Keller / Jaeksoft
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensearchserver.pojodbc;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModel;

import com.opensearchserver.pojodbc.connection.ConnectionManager;

public abstract class PageDataModel<T> extends DataModel<T> {

	private int currentStart;
	private int currentIndex;
	private int pageSize;
	private int size;
	private List<T> list;
	private Object data;
	private Class<T> beanClass;
	private ConnectionManager connectionManager;
	private int transactionIsolation;

	public PageDataModel(ConnectionManager cm, int transactionIsolation,
			Class<T> beanClass, int pageSize) throws SQLException {
		this.connectionManager = cm;
		this.transactionIsolation = transactionIsolation;
		this.list = null;
		this.currentStart = -1;
		this.currentIndex = -1;
		this.pageSize = pageSize;
		this.size = 0;
		this.beanClass = beanClass;
	}

	private boolean needUpdate(long index) {
		synchronized (this) {
			return index < currentStart || index >= currentStart + pageSize;
		}
	}

	@Override
	public int getRowCount() {
		synchronized (this) {
			return size;
		}
	}

	@Override
	public T getRowData() {
		synchronized (this) {
			if (currentIndex == -1 || currentStart == -1)
				return null;
			return list.get(currentIndex - currentStart);
		}
	}

	@Override
	public int getRowIndex() {
		synchronized (this) {
			return (int) currentIndex;
		}
	}

	@Override
	public Object getWrappedData() {
		synchronized (this) {
			return data;
		}
	}

	@Override
	public boolean isRowAvailable() {
		synchronized (this) {
			if (needUpdate(currentIndex))
				return false;
			long realIndex = currentIndex - currentStart;
			if (realIndex < 0)
				return false;
			if (realIndex >= list.size())
				return false;
			return true;
		}
	}

	@Override
	public void setRowIndex(int index) {
		synchronized (this) {
			currentIndex = index;
			if (index == -1)
				return;
			if (needUpdate(index))
				try {
					populate(index);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
		}
	}

	@Override
	public void setWrappedData(Object data) {
		synchronized (this) {
			this.data = data;
		}
	}

	public void populate(int index) throws Exception {
		synchronized (this) {
			if (index == currentStart)
				return;
			Transaction transaction = null;
			try {
				transaction = connectionManager.getNewTransaction(false,
						transactionIsolation);
				Query query = getQuery(transaction);
				query.setFirstResult(index);
				query.setMaxResults(pageSize);
				list = query.getResultList(beanClass);
				size = query.getResultCount();
				currentStart = index;
			} catch (Exception e) {
				throw e;
			} finally {
				if (transaction != null)
					transaction.close();
			}
		}
	}

	public abstract Query getQuery(Transaction transaction);

	public Iterator<T> currentPageIterator() {
		synchronized (this) {
			return list.iterator();
		}
	}

}
