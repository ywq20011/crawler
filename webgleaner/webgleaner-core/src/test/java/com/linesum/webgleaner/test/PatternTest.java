/**
 * 
 */
package com.linesum.webgleaner.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * 
 * @author linjx
 * @Date 2016年3月10日
 * @Version 1.0.0
 */
public class PatternTest {

	@Test
	public void patternTest() throws IOException {
		Reader reader = new InputStreamReader(
				FileUtils.openInputStream(new File("E:\\test\\spider\\shopsearch.taobao.com\\test.html")));
		StringBuilder sb = new StringBuilder();
		int tempChar;
		while ((tempChar = reader.read()) != -1) {
			sb.append((char) tempChar);
		}
		reader.close();
		System.out.println(sb.toString());

		String regex = ".*g_page_config";
		
		Pattern pattern = Pattern.compile(regex);
		System.out.println(pattern.matcher(sb.toString()).group());

//		PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(""))));
//		printWriter.println();
//		printWriter.close();

		
	}

}
