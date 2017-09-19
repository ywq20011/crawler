package com.linesum.webgleaner.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

public class RandomGeneratorTest {

	@Test
	public void testMathRandom() throws IOException {
		List<String> lines = new ArrayList<String>();
		for (int i = 0; i < 1e4; i ++ ) {
			lines.add(String.valueOf(Math.random()));
		}
		FileUtils.writeLines(new File("D:\\random.txt"), lines);
	}
	
	@Test
	@Ignore
	public void testRandomClass() {
		Random random = new Random();
		for (int i = 0 ; i < 1e3; i ++) {
			System.out.println(random.nextInt(100));
		}
	}
}
