package us.codecraft.webmagic.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author yxssfxwzy@sina.com May 30, 2014
 * 
 */
public class ProxyTest {

	private static List<ProxyIP> httpProxyList = new ArrayList<ProxyIP>();

	@BeforeClass
	public static void before() {
		ProxyIP pp01 = new ProxyIP("182.90.16.34", 80);
		ProxyIP pp02 = new ProxyIP("182.90.16.35", 80);
		ProxyIP pp03 = new ProxyIP("182.90.16.36", 80);
		ProxyIP pp04 = new ProxyIP("182.90.16.37", 80);
		pp01.setIsHttps(true);
		httpProxyList.add(pp01);
		httpProxyList.add(pp02);
		httpProxyList.add(pp03);
		httpProxyList.add(pp04);
	}

	@SuppressWarnings("unused")
	@Test
	public void testProxy() throws InterruptedException {
		ProxyPool proxyPool = new ProxyPool(httpProxyList, false, "D:\\test\\lastUsed.proxy");
		proxyPool.setReuseInterval(500);
		assertThat(proxyPool.getIdleNum()).isEqualTo(4);
		assertThat(new File(proxyPool.getProxyFilePath()).exists()).isEqualTo(true);
		for (int i = 0; i < 2; i++) {
			List<Fetch> fetchList = new ArrayList<Fetch>();
			int j = 0;
			while (!proxyPool.isEmptyHttpsProxy()) {
				HttpHost httphost = proxyPool.getProxy(true);
				if (httphost != null) {
					System.out.println(httphost.getHostName() + ":" + httphost.getPort());
					Fetch tmp = new Fetch(httphost);
					tmp.start();
					fetchList.add(tmp);
				}
				Thread.sleep(200);
			}
			for (Fetch fetch : fetchList) {
				proxyPool.returnProxy(fetch.hp, Proxy.SUCCESS, "");
			}
			System.out.println(proxyPool.allProxyStatus());
		}
	}

	class Fetch extends Thread {
		HttpHost hp;

		public Fetch(HttpHost hp) {
			this.hp = hp;
		}

		@Override
		public void run() {
			try {
				System.out.println("fetch web page use proxy: " + hp.getHostName() + ":" + hp.getPort());
				sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
