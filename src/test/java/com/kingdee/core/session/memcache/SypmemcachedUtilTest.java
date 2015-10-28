package com.kingdee.core.session.memcache;

import java.io.IOException;

import net.spy.memcached.MemcachedClient;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SypmemcachedUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetMemcachedClient() throws IOException {
		MemcachedClient c = SypmemcachedUtil.getMemcachedClient("/config/mcloud.properties");
		c.set("test", 100, 1);
	}

}
