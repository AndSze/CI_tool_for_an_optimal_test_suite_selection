package MockitoTutorialBusiness;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void letsMockListSizeMethod_ReturnMultipleValues() {
		List listMock = mock(List.class);
		when(listMock.size()).thenReturn(2).thenReturn(3);
		assertEquals(2, listMock.size());
		assertEquals(3, listMock.size());
	}
	
	@Test
	public void letsMockListGetMethod_ReturnMultipleValues() {
		List listMock = mock(List.class);
		when(listMock.get(anyInt())).thenReturn("abc").thenReturn(1);
		assertEquals("abc", listMock.get(0));
		assertEquals(1, listMock.get(152));
		//assertEquals(null, listMock.get(1));
	}
	
	@Test(expected=RuntimeException.class)
	public void letsMockListGetMethod_ReturnException() {
		List listMock = mock(List.class);
		when(listMock.get(anyInt())).thenThrow(new RuntimeException("New Exception"));
		
		// this will throw an exception
		listMock.get(0);
	}

}
