package com.kingdee.core.session.memcache;


import javax.servlet.http.HttpSession;

/**
 * 
 * 
 *
 * Power by -10 (shi_hu@kingdee.com)
 * Jun 23, 2014
 */
public interface HttpSessionManager {

	public HttpSession getSession(String sessionId);

	public boolean exists(String sessionId);

	public HttpSession newSession(String sessionId);

	public void updateSession(HttpSession session);

	public String getSessionKey();

	public int getSessionTimeout();

	public String getCookieDomain();

	public String getCookiePath();
}
