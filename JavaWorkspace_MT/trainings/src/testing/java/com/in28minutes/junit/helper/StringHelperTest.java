package com.in28minutes.junit.helper;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class StringHelperTest {
	
	StringHelper helper = new StringHelper(99);
	
	@Before
	public void before() {
		helper = new StringHelper(0);
	}
	
	@Test
	public void testTruncateAInFirst2Positions1() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		System.out.println(helper.getID());
		helper.setID(helper.getID()+1);
		System.out.println(helper.getID());
		assertEquals("CD", helper.truncateAInFirst2Positions("AACD"));
	}
	
	@Test
	public void testTruncateAInFirst2Positions2() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		System.out.println(helper.getID());
		assertEquals("CD", helper.truncateAInFirst2Positions("ACD"));
	}
	
	@Test
	public void testTruncateAInFirst2Positions3() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		System.out.println(helper.getID());
		assertEquals("CDEF", helper.truncateAInFirst2Positions("CDEF"));
	}
	
	@Test
	public void testTruncateAInFirst2Positions4() {
		// AACD => CD ACD => CD CDEF => CDEF CDAA => CDAA
		System.out.println(helper.getID());
		assertEquals("CDAA", helper.truncateAInFirst2Positions("CDAA"));
	}
	
	@Test
	public void testAreFirstAndLastTwoCharactersTheSame1() {
		// ABCE => false, ABAB => true, AB => true, A => false
		assertFalse(helper.areFirstAndLastTwoCharactersTheSame("ABCE"));
	}
	
	@Test
	public void testAreFirstAndLastTwoCharactersTheSame2() {
		// ABCE => false, ABAB => true, AB => true, A => false
		assertTrue(helper.areFirstAndLastTwoCharactersTheSame("ABAB"));
	}
	
	@Test
	public void testAreFirstAndLastTwoCharactersTheSame3() {
		// ABCE => false, ABAB => true, AB => true, A => false
		assertTrue(helper.areFirstAndLastTwoCharactersTheSame("AB"));
	}
	
	@Test
	public void testAreFirstAndLastTwoCharactersTheSame4() {
		// ABCE => false, ABAB => true, AB => true, A => false
		assertFalse("A => false", helper.areFirstAndLastTwoCharactersTheSame("A"));
	}
}
