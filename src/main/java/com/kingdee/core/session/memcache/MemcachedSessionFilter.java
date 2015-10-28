package com.kingdee.core.session.memcache;

import java.util.logging.Logger;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 
 * 
 *
 * Power by -10 (shi_hu@kingdee.com)
 * Jun 23, 2014
 */
public class MemcachedSessionFilter extends OncePerRequestFilter {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(MemcachedSessionFilter.class.getName());
	
	public static final String HOST_NAME_REGEX = "^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])$";

	private HttpSessionManager sessionManager;
	private String sessionSuffix;

	private String mixSessionId(String sessionIdInCookie, HttpServletRequest req) {
		if (StringUtils.isEmpty(sessionIdInCookie)) {
			return null;
		}
		// sessionIdInCookie += req.getRemoteAddr();
		// return DigestUtils.md5Hex(sessionIdInCookie);
		// 不能绑定ip地址，因为有些网络是双线或者多线的，每次请求的ip都不一样
		return sessionIdInCookie;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		final String sessionKey = sessionManager.getSessionKey();
		final String cookiePath = sessionManager.getCookiePath();
		int sessionTimeout = sessionManager.getSessionTimeout();
		// 从cookie中取出sessionId
		Cookie cookies[] = req.getCookies();
		String sessionIdInCookie = null;
		boolean sessionFromCookie = true;
		
		/**
		 * TODO 若是 浏览器 记录了多个  cookie, 即 无法跟新  cookie ?
		 */
		if (cookies != null && cookies.length > 0) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(sessionKey)) {
					sessionIdInCookie = cookies[i].getValue();
					break ;
				} 
			}
		}
		if (sessionIdInCookie == null) {
			sessionIdInCookie = parseSessionIdFromUri(req);
			sessionFromCookie = false;
		}
		
		String sessionId = mixSessionId(sessionIdInCookie, req);
		HttpSession session = sessionManager.getSession(sessionId);
		if (session == null) {
			
			/**
			 * 记录拿不到 session 时的 sid 和 当时的 cookie 情况
			 */
			logger.warning("session is null, sid = " + sessionId);
			logger.warning("url: " + req.getRequestURL());
			logger.warning("Cookie :" + req.getHeader("Cookie"));
			
			String newSessionId = RandomStringUtils.randomAlphanumeric(32);
			// 混淆sessionId
			sessionId = mixSessionId(newSessionId, req);
			session = sessionManager.newSession(sessionId);
			Cookie sessionCookie = new Cookie(sessionKey, newSessionId);
			
			// 设置 cookie path 和 domain
			sessionCookie.setPath(cookiePath);
			
			sessionCookie.setHttpOnly(true);
			
			String domain = parseCookieTopDomain(req.getServerName());
	        if(StringUtils.isNotBlank(domain)) {
	        	sessionCookie.setDomain(domain);
	        }
	        
			res.addCookie(sessionCookie);
		}
		session.setMaxInactiveInterval(sessionTimeout);

		req = new HttpSessionRequestWrapper(req, session);
		res = new HttpSessionResponseWrapper(res, session, sessionManager, sessionFromCookie);
		
		
		try {
			/**
			 * TODO sid 跨域写 cookie
			 */
			res.setHeader("P3P","CP=CAO PSA OUR");
			
			chain.doFilter(req, res);
			
		} finally {
			/**
			 * TODO 保证 session 每次进入的时候都能保持更新,
			 * 比放在  doFilter 之前性能更高 
			 */
			sessionManager.updateSession(session);
		}
	}

	@Override
	protected void initFilterBean() throws ServletException {
		
		sessionManager = MemcachedSessionManager.getInstance();
		
		/**
		 * TODO 若是使用 spring 应该去掉这里的注入
		 */
		((MemcachedSessionManager)sessionManager).setServletContext(this.getServletContext());
//		String rootPath = this.getServletContext().getRealPath("/config/mcloud.properties");
//		System.out.println("web root path" + rootPath);
//		((MemcachedSessionManager)sessionManager).setMemcachedClient(SypmemcachedUtil.getMemcachedClient(rootPath));
//		((MemcachedSessionManager)sessionManager).setMemcachedClient(SypmemcachedUtil.getMemcachedClient("/config/mcloud.properties"));
		
		String realPath = this.getServletContext().getRealPath("/") + "WEB-INF";
				
		//		String clazz = this.getClass().getClassLoader().getResource("").getFile();
		//		File rootPath = new File(clazz).getAbsoluteFile().getParentFile();
		//		System.out.println(rootPath.getAbsolutePath());
		
		((MemcachedSessionManager)sessionManager).setMemcachedClient(SypmemcachedUtil.getMemcachedClient(realPath + "/config/im.properties"));
		
		sessionSuffix = ";" + sessionManager.getSessionKey() + "=";
	}

	@Override
	public void destroy() {
		sessionManager = null;
	}

	private String parseSessionIdFromUri(HttpServletRequest request) {
		String sessionId = null;
		String uri = request.getRequestURI();
		int p = uri.indexOf(sessionSuffix);
		if (p >= 0) {
			int suffixLength = sessionSuffix.length();
			int tail = uri.indexOf(';', p + suffixLength);
			if (tail > 0)
				sessionId = uri.substring(p + suffixLength, tail);
			else
				sessionId = uri.substring(p + suffixLength);
		}
		return sessionId;
	}
	
	/**
     * 将域名设置顶级域名上，兼容开发环境的机器名或ip模式
     * test.kdweibo.cn返回.kdweibo.cn;
     * kdweibo.com返回.kdweibo.com
     * 192.168.1.221 返回 Null
     * localhost返回Null
     * @param request
     * @return
     */
    private static String parseCookieTopDomain(String serverName) {
        if(isSubDomain(serverName)) {
            String[] fregments =  StringUtils.split(serverName, '.');
            StringBuffer topDomain= new StringBuffer();
            //FIXME 添加Guava的InternetDomainName处理之前,先用比较挫的方法解决紧急问题
            if(serverName.endsWith(".com.cn") || serverName.endsWith(".com.hk") || serverName.endsWith(".com.tw")){
                topDomain.append(".").append(fregments[fregments.length-3]).append(".").append(fregments[fregments.length-2])
                .append(".").append(fregments[fregments.length-1]);
            }else{
                topDomain.append(".").append(fregments[fregments.length-2])
                .append(".").append(fregments[fregments.length-1]);            	
            }
            return topDomain.toString();
        }
        return null;
    }
    
    public static boolean isSubDomain(String serverName){
        if(!StringUtils.isEmpty(serverName) && serverName.matches(HOST_NAME_REGEX)) {
            String[] fregments =  StringUtils.split(serverName, '.');
            if(fregments.length>1) {
                return true;
            }
        }
        return false;
    }
}
