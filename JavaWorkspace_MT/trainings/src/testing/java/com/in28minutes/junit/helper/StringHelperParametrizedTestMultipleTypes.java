package com.in28minutes.junit.helper;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StringHelperParametrizedTestMultipleTypes {

	StringHelper helper = new StringHelper(99);
	
	private String input;
	private boolean expectedOutput;
	
	public StringHelperParametrizedTestMultipleTypes(String input, boolean expectedOutput) {
		this.input = input;
		this.expectedOutput = expectedOutput;
	}

	@Before
	public void before() {
		helper = new StringHelper(0);
	}
	
	 @Parameters
    public static Collection<Object[]> data(){
        return Arrays.asList(new Object[][] {
          {"AACD", false},
          {"ABAB", true},
          {"AB", true},
          {"A", false},
        });
    }

	@Test
	public void testAreFirstAndLastTwoCharactersTheSame() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		assertEquals(expectedOutput, helper.areFirstAndLastTwoCharactersTheSame(input));
	}
}
