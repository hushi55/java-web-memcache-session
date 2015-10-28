package com.kingdee.core.session.memcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;

import org.apache.commons.lang3.StringUtils;

public class SypmemcachedUtil {
	
	private static final String IM_SPYMEMCACHE_SERVERS 							= "im.spymemcache.servers";
	private static final String IM_SPYMEMCACHE_COMPRESSION_THRESHOLD 			= "im.spymemcache.compressionThreshold";
	private static final String IM_SPYMEMCACHE_TIMEOUT_EXCEPTION_THRESHOLD 		= "im.spymemcache.timeoutExceptionThreshold";
	private static final String IM_SPYMEMCACHE_OP_TIMEOUT 						= "im.spymemcache.opTimeout";
	private final static ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder();
	private static MemcachedClient client;
	
	public static MemcachedClient getMemcachedClient(String prop) {
		
		
		if (client == null) {
			synchronized (SypmemcachedUtil.class) {
				if (client == null) {
					
					if (StringUtils.isBlank(prop)) {
						prop = "/META-INF/spring/spymemcache.properties";
					}
					
					try {
						Properties properties = new Properties();
						
						InputStream is = new FileInputStream(new File(prop));
						properties.load(is);
//						properties.load(SypmemcachedUtil.class.getResourceAsStream(prop));
						
						connectionFactoryBuilder.setProtocol(Protocol.BINARY);
						connectionFactoryBuilder.setOpTimeout(Long.valueOf(properties.getProperty(IM_SPYMEMCACHE_OP_TIMEOUT)));
						connectionFactoryBuilder.setTimeoutExceptionThreshold(Integer.valueOf(properties.getProperty(IM_SPYMEMCACHE_TIMEOUT_EXCEPTION_THRESHOLD)));
						connectionFactoryBuilder.setHashAlg(DefaultHashAlgorithm.KETAMA_HASH);
						connectionFactoryBuilder.setLocatorType(Locator.CONSISTENT);
						connectionFactoryBuilder.setFailureMode(FailureMode.Redistribute);
						connectionFactoryBuilder.setUseNagleAlgorithm(false);
						
						SerializingTranscoder t = new SerializingTranscoder();
						t.setCompressionThreshold(Integer.valueOf(properties.getProperty(IM_SPYMEMCACHE_COMPRESSION_THRESHOLD)));
						connectionFactoryBuilder.setTranscoder(t);
						
						String servers = properties.getProperty(IM_SPYMEMCACHE_SERVERS);
						if (StringUtils.isNotBlank(System.getProperty("memcached.servers"))) {
							servers = System.getProperty("memcached.servers");
						}
						client = new MemcachedClient(
								connectionFactoryBuilder.build(),
						        AddrUtil.getAddresses(servers));
					} catch (NumberFormatException e) {
						e.printStackTrace();
						new RuntimeException(e.getMessage(), e);
					} catch (IOException e) {
						e.printStackTrace();
						new RuntimeException(e.getMessage(), e);
					}
				}
			}
		}

		
		return client;
	}

}
