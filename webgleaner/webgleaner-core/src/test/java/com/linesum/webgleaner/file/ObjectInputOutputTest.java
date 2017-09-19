package com.linesum.webgleaner.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpHost;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hlg.webgleaner.core.proxy.HttpProxy;

public class ObjectInputOutputTest {
	
	public static Map<String, HttpProxy> allProxy = new ConcurrentHashMap<String, HttpProxy>();
	
	@BeforeClass
	public static void start() throws UnknownHostException {
		allProxy.put("192.168.0.1", new HttpProxy(new HttpHost(InetAddress.getByName("192.168.0.1")), 80));
		allProxy.put("192.168.0.2", new HttpProxy(new HttpHost(InetAddress.getByName("192.168.0.2")), 80));
		allProxy.put("192.168.0.3", new HttpProxy(new HttpHost(InetAddress.getByName("192.168.0.3")), 80));
		allProxy.put("192.168.0.4", new HttpProxy(new HttpHost(InetAddress.getByName("192.168.0.4")), 80));
	}
	
	@Test
	@Ignore
	public void testObjectOutput() throws IOException {
		String filePath = "D:\\test\\object.txt";
		File file = new File(filePath);
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.mkdirs();
			}
			file.createNewFile();
		}
		
		ObjectOutputStream objectOutp = new ObjectOutputStream(new FileOutputStream(file));
		objectOutp.writeObject(allProxy);
		objectOutp.flush();
		objectOutp.close();
		System.out.println("finished ...");
	}
	
	@SuppressWarnings("unchecked")
	@Test
//	@Ignore
	public void testObjectIn() throws FileNotFoundException, IOException, ClassNotFoundException {
		String filePath = "D:\\test\\object.txt";
		File file = new File(filePath);
		
		ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream(file));
		Map<String, HttpProxy> allProxy = (Map<String, HttpProxy>) objectIn.readObject();
		if (MapUtils.isNotEmpty(allProxy)) {
			for (Entry<String, HttpProxy> entry : allProxy.entrySet()) {
				System.out.println(entry.getValue());
			}
		}
		objectIn.close();
	}

}
