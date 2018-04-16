package com.in28minutes.junit.helper;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StringHelperParametrizedTest {

	StringHelper helper = new StringHelper(99);
	
	private String input;
	private String expectedOutput;
	
	public StringHelperParametrizedTest(String input, String expectedOutput) {
		this.input = input;
		this.expectedOutput = expectedOutput;
	}

	@Before
	public void before() {
		helper = new StringHelper(0);
	}
	
	@Parameters
	public static Collection testConditions() {
		// {input , expected output}
		String expectedOutputs[][] = { {"AACD", "CD"},
				{"ACD", "CD"},
				{"CDEF", "CDEF"},
				{"CDAA", "CDAA"} };
		
		return Arrays.asList(expectedOutputs);
	}
	
	@Test
	public void testTruncateAInFirst2Positions() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		assertEquals(expectedOutput, helper.truncateAInFirst2Positions(input));
	}
}


