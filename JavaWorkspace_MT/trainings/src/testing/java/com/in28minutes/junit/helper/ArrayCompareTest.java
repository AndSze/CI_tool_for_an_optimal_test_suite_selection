package com.in28minutes.junit.helper;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public class ArrayCompareTest {

	@Test
	public void testArraySort_RandomArray() {
		int[] numbers = {12,3,4,1,};
		int[] expected = {1,3,4,12};
		Arrays.sort(numbers);
		assertArrayEquals(expected,numbers);
		// arrays first differed at element [0]
		// expected:<2> but was:<1>
	}
	
	@Test(expected = NullPointerException.class)
	public void testArraySort_NullArray() {
		
		int[] numbers = null;
		try {
			Arrays.sort(numbers);
		} catch (NullPointerException e) {
			Arrays.sort(numbers);
		}
	}
	
	@Test(timeout = 30) //ms
	public void test_sort_Performance() {
		int array[] = {12,23,4, 34, 54, 75};
		for (int i = 0; i < 1000000; i++) {
			array[0] = i;
			Arrays.sort(array);;
		}
	}

}



