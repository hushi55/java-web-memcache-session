package com.kingdee.core.session.memcache;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * 
 * 
 *
 * Power by -10 (shi_hu@kingdee.com)
 * Jun 23, 2014
 */
public class HttpSessionRequestWrapper extends HttpServletRequestWrapper {

	private HttpSession session;

	public HttpSessionRequestWrapper(HttpServletRequest request, HttpSession session) {
		super(request);
		this.session = session;
	}

	public HttpSession getSession(boolean create) {
		return session;
	}

	public HttpSession getSession() {
		return getSession(true);
	}
}
