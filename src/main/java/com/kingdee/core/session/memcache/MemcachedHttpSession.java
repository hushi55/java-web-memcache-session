package com.kingdee.core.session.memcache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;

public class MemcachedHttpSession implements HttpSession, Serializable {

	private static final int RETRY_COUNT = 3;

	private static final long serialVersionUID = 6085773901132571541L;

	private final MemcachedClient memcachedClient;

	private final String sessionId;

	private final Map<String, Object> sessionValueMap = new HashMap<String, Object>();
	private final long casId;

	private int maxInactiveInterval;

	private ServletContext servletContext;

	private boolean updated = false;

	private boolean invalidated = false;

	@SuppressWarnings("unchecked")
	public MemcachedHttpSession(String sessionId,
			MemcachedClient memcachedClient) {
		this.sessionId = sessionId;
		this.memcachedClient = memcachedClient;
		CASValue<Object> cas = memcachedClient.gets(sessionId);
		if (cas != null) {
			this.casId = cas.getCas();
			Object o = cas.getValue();
			if (o != null && o instanceof Map<?, ?>) {
				sessionValueMap.putAll((Map<String, Object>) o);
			}
		} else {
			this.casId = 0;
		}
	}

	/**
	 * 批量序列化
	 */
	@SuppressWarnings("unchecked")
	protected void update() {

		// TODO 保证每次都有更新
		if (invalidated) {
			return;
		}
		if (sessionValueMap.size() == 0) {
			// 空会话不需要序列号，提高性能
			memcachedClient.set(sessionId, getMaxInactiveInterval(), 0);
		} else {
			CASResponse r = this.memcachedClient.cas(sessionId, casId,
					getMaxInactiveInterval(), sessionValueMap);

			int loop = RETRY_COUNT; // 尝试 3 次

			while (CASResponse.EXISTS == r && loop > 0) {

				loop--;

				CASValue<Object> cas = this.memcachedClient
						.gets(this.sessionId);
				if (cas != null && cas.getValue() instanceof Map<?, ?>) {
					sessionValueMap
							.putAll((Map<String, Object>) cas.getValue());
					r = this.memcachedClient.cas(sessionId, cas.getCas(),
							getMaxInactiveInterval(), sessionValueMap);
					

				}
			}
		}
		/**
		 * 每次都将 map 写入
		 */
		// @SuppressWarnings("unused")
		// OperationFuture<Boolean> f = memcachedClient.set(sessionId,
		// getMaxInactiveInterval(), sessionValueMap);
		// updated = true;
		// try {
		// if (f.get()) { // 保证写入操作都是成功了的
		// updated = true;
		// }
		// } catch (InterruptedException e) {
		// // TODO ignore ?
		// } catch (ExecutionException e) {
		// // TODO ignore ?
		// }
	}

	protected Map<String, Object> getValueMap() {
		return this.sessionValueMap;
	}

	public Object getAttribute(String key) {
		return this.sessionValueMap.get(key);
	}

	public Enumeration<String> getAttributeNames() {
		return (new Enumerator<String>(this.sessionValueMap.keySet(), true));
	}

	public void invalidate() {
		memcachedClient.delete(sessionId);
		updated = true;
		invalidated = true;
	}

	/**
	 * TODO 若是一个请求时间太长，后面的请求又依赖这个 session，可能数据不能及时同步
	 */
	public void removeAttribute(String key) {
		sessionValueMap.remove(key);
		// TODO 提高性能
//		update();
		this.updated = true;
	}

	public void setAttribute(String key, Object value) {
		sessionValueMap.put(key, value);
//		update();
		this.updated = true;
	}

	@Override
	public long getCreationTime() {
		return 0;
	}

	@Override
	public String getId() {
		return sessionId;
	}

	@Override
	public long getLastAccessedTime() {
		return 0;
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Deprecated
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		return null;
	}

	@Override
	@Deprecated
	public Object getValue(String key) {
		return getAttribute(key);
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		Set<String> set = sessionValueMap.keySet();
		return set.toArray(new String[set.size()]);
	}

	@Override
	public boolean isNew() {
		return false;
	}

	@Override
	@Deprecated
	public void putValue(String key, Object value) {
		setAttribute(key, value);
	}

	@Override
	@Deprecated
	public void removeValue(String key) {
		removeAttribute(key);
	}

	@Override
	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}

class Enumerator<E> implements Enumeration<E> {

	public Enumerator(Collection<E> collection) {
		this(collection.iterator());
	}

	public Enumerator(Collection<E> collection, boolean clone) {
		this(collection.iterator(), clone);
	}

	public Enumerator(Iterator<E> iterator) {
		super();
		this.iterator = iterator;
	}

	public Enumerator(Iterator<E> iterator, boolean clone) {
		super();
		if (!clone) {
			this.iterator = iterator;
		} else {
			List<E> list = new ArrayList<E>();
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
			this.iterator = list.iterator();
		}
	}

	public Enumerator(Map<?, E> map) {
		this(map.values().iterator());

	}

	public Enumerator(Map<?, E> map, boolean clone) {
		this(map.values().iterator(), clone);
	}

	private Iterator<E> iterator = null;

	public boolean hasMoreElements() {
		return (iterator.hasNext());
	}

	public E nextElement() throws NoSuchElementException {
		return (iterator.next());
	}
}