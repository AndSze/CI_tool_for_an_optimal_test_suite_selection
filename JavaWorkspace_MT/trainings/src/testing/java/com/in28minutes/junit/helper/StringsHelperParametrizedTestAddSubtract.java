package com.in28minutes.junit.helper;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StringsHelperParametrizedTestAddSubtract {

	private StringHelper helper = new StringHelper(99);
	enum Type {SUBSTRACT, ADD};
	private Type type;
	private double a, b, expected, delta;
	
	// constructor has to correspond to the order of parameters in Arrays.asList(new Object[][] .... )
	public StringsHelperParametrizedTestAddSubtract( Type type, double a, double b, double expected, double delta) {
		this.a = a; this.b = b; this.expected = expected; this.delta = delta;
		this.type = type;
	}

	@Before
	public void before() {
		helper = new StringHelper(0);
	}
	
	@Parameters
    public static Collection<Object[]> data(){
        return Arrays.asList(new Object[][] {
        	{Type.SUBSTRACT, 3.0, 2.0, 1.0, 0.000000001},
            {Type.ADD, 23.0, 5.0, 28.0, 0.000000001}
        });
    }

	@Test 
	public void testAdd() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		Assume.assumeTrue(type == Type.ADD); // org.junit.AssumptionViolatedException: got: <false>, expected: is <true>
		assertEquals(expected, helper.add(a, b), delta);
	}

	@Test
    public void testSubstract(){
        Assume.assumeTrue(type == Type.SUBSTRACT);
        assertEquals(expected, helper.substract(a, b), delta);
    }
	
   @After
    public void teardown() {
        a = 0;
        b = 0;
        expected = 0;
    }
}
