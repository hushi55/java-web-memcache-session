package com.kingdee.core.session.memcache;
/** filename:OnlineUserFilter.java */


import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * <pre>
 * 通过用户是否调用getSession接口，判断用户是否在线
 * </pre>
 * 
 *
 * Power by -10 (shi_hu@kingdee.com)
 * Jun 23, 2014
 */
public class OnlineUserFilter implements Filter {

    private static JedisPool jedisPool;

    private static final int EXPIRE_SECOND = 1200; // redis key默认过期时间 TODO 用spring config配置,单位秒
    private static final String Default_VALUE = "1";
    private static final String KEY_PREFIX = "HB";
    private static final Executor executor = Executors.newFixedThreadPool(20);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession();
        
        //TODO
        /**
         * user session key
         */
        final Long currentUserId = (Long) session.getAttribute("__session_user");
        if (currentUserId != null) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    if (jedisPool != null) {
                        Jedis jedis = null;
                        try {
                            jedis = jedisPool.getResource();
                            jedis.setex(geneKey(KEY_PREFIX, String.valueOf(currentUserId)), EXPIRE_SECOND, Default_VALUE);
                        } catch (Exception e) {
                            jedisPool.returnBrokenResource(jedis);
                        } finally {
                            jedisPool.returnResource(jedis);// 释放链接到jedispool
                        }
                    }
                }
            });

        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        WebApplicationContext spring = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
        jedisPool = (JedisPool) spring.getBean("jedisPool");
    }

    @Override
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.destroy();
        }
    }

    private static String geneKey(String preifx, String key) {
        return preifx + ":" + key;
    }

    /**
     * 当前用户是否在线
     */
    public static boolean isOnline(long userId) {
        if (jedisPool != null) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                String s = jedis.get(geneKey(KEY_PREFIX, String.valueOf(userId)));
                if (!StringUtils.isEmpty(s) && s.equals("1")) {
                    return true;
                }
            } catch (Exception e) {
                jedisPool.returnBrokenResource(jedis);
            } finally {
                jedisPool.returnResource(jedis);// 释放链接到jedispool
            }
        }
        return false;
    }

    /**
     * 当前在线用户总数
     */
    public static long totalOnline() {
        long total = 0;
        if (jedisPool != null) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                total = jedis.keys("HB*").size();
            } catch (Exception e) {
                jedisPool.returnBrokenResource(jedis);
            } finally {
                jedisPool.returnResource(jedis);// 释放链接到jedispool
            }
        }
        return total;
    }
}
