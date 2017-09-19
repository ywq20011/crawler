/**
 * 
 */
package com.linesum.webgleaner.test;

import java.io.File;

import org.junit.Test;

/**
 * 
 * @author linjx
 * @Date 2016年3月8日
 * @Version 1.0.0
 */
public class ScanDirectoryTest {
	
	@Test
	public void scanAllDirectory() {
		//File roots[] = File.listRoots(); //系统下所有根路径
		File roots[] = new File[] {new File("D:\\"), new File("E:\\"), new File("F:\\")};
		if (null != roots && roots.length > 0) {
			for (File f : roots) {
				check(f);
			}
		}
	}
	
	private void check(File f) {
		if (null != f) {
			System.out.println(f.toString());
			if (f.isDirectory()) {
				File[] fs = f.listFiles();
				if (fs != null) {
					for (File subF : fs) {
						check(subF); //递归
					}
				}
			}
		}
	}

	
}
