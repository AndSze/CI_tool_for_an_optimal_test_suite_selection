package com.in28minutes.junit.helper;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QuickBeforAfterTest {

	@BeforeClass
	public static void beforeClass() {
		System.out.println("Before Class");
	}
	
	@Before
	public void setup() {
		System.out.println("Before Test");
	}
	
	@Test
	public void test1() {
		System.out.println("Test1 Executed");
	}
	
	@Test
	public void test2() {
		System.out.println("Test2 Executed");
		String expectedOutputs[][] = { {"AACD", "CD"},
				{"ACD", "CD"},
				{"CDEF", "CDEF"},
				{"CDAA", "CDAA"} };
		System.out.println(Arrays.asList(expectedOutputs));
	}
	
	@After
	public void cleanup() {
		System.out.println("After Test");
	}

	@AfterClass
	public static void afterClass() {
		System.out.println("After Class");
	}
}
