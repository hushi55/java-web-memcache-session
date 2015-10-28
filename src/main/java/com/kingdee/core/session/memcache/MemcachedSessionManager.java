package com.kingdee.core.session.memcache;


import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

@Service
public class MemcachedSessionManager implements HttpSessionManager, ServletContextAware {

	@Resource
	private MemcachedClient memcachedClient;

	private ServletContext servletContext;
	
	private static MemcachedSessionManager _instance = new MemcachedSessionManager();

	/**
	 * 修改名称，防止 sid 被用来当做参数
	 */
	public String sessionKey = "__m_sid";//修改 sid 的 key
	
	// 2 hous
	private int sessionTimeout = 60 * 60 * 2; //in seconds
	
	public String cookieDomain = ".kdweibo.com";
	public String cookiePath = "/";
	
	private MemcachedSessionManager(){
		_instance = this;
	}
	
	public static MemcachedSessionManager getInstance() {
		return _instance;
	}

	@Override
	public MemcachedHttpSession newSession(String sessionId) {
		MemcachedHttpSession session = new MemcachedHttpSession(sessionId, memcachedClient);
		session.setServletContext(servletContext);
		return session;
	}

	public MemcachedHttpSession getSession(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
			return null;
		}
//		MemcachedClient client = this.memcachedClient;
//
//		Object object = client.get(sessionId);
//		if (object == null) {
//			return null;
//		}
//		MemcachedHttpSession session = newSession(sessionId);
//		if (object instanceof Map<?, ?>) {
//			@SuppressWarnings("unchecked")
//			Map<String, Object> sessionMap = (Map<String, Object>) object;
//			session.getValueMap().putAll(sessionMap);
//		}
		return newSession(sessionId);
	}

	@Override
	public boolean exists(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
			return false;
		}
		return memcachedClient.get(sessionId) != null;
	}

	@Override
	public void updateSession(HttpSession session) {
		MemcachedHttpSession s = (MemcachedHttpSession) session;
		if (s != null && s.isUpdated()) { //若是没有更新，执行更新操作
			s.update();
		}
//		s.update();
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public String getSessionKey() {
		return sessionKey;
	}

	@Override
	public int getSessionTimeout() {
		return sessionTimeout;
	}

	@Override
	public String getCookieDomain() {
		return cookieDomain;
	}

	@Override
	public String getCookiePath() {
		return cookiePath;
	}

	public void setMemcachedClient(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}
}
