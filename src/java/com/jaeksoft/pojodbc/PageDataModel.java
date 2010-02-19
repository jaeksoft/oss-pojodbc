/**   
 * License Agreement for Jaeksoft Pojodbc
 *
 * Copyright (C) 2008-2010 Emmanuel Keller / Jaeksoft
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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModel;

import com.jaeksoft.pojodbc.connection.ConnectionManager;

public abstract class PageDataModel<T> extends DataModel {

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
	public Object getRowData() {
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

	@SuppressWarnings("unchecked")
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
				list = (List<T>) query.getResultList(beanClass);
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
