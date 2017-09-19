package com.linesum.webgleaner.proxy;

import org.junit.Test;

import com.hlg.webgleaner.core.proxy.ProxyGetter;

public class ProxyApiTest {
	
	@Test
	public void testProxyAPI() {
		ProxyGetter.getProxyByAPI(true, 2, "559809259830430");
	}

}
