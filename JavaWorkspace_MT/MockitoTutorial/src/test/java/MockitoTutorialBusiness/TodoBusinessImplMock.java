package MockitoTutorialBusiness;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import MockitoTutorialSrc.TodoService;
import MockitoTutorialSrc.TodoServiceStub;

public class TodoBusinessImplMock {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRetrieveTodosRelatedToSpring_usingAMock() {
		
		// Comparison between Stubs and Mocks
		//TodoService mockTodoService = mock(TodoService.class);
		//stub(mockTodoService.retrieveTodos("Parameter")).return("value");
		
		TodoService todoServiceMock = mock(TodoService.class);
		
		// sth that acts as stub - we're defining what to do in the mock
		List<String> todos = Arrays.asList("Learn Spring MVC", "Learn Spring", "Learn to Dance");
		when(todoServiceMock.retrieveTodos("Dummy")).thenReturn(todos);
		
		// when =~ stub
		
		TodoBusinessImpl todoBusinessImpl = new TodoBusinessImpl(
				todoServiceMock);
		
		List<String> filteredTodos = todoBusinessImpl
				.retrieveTodosRelatedToSpring("Dummy");
		
		assertEquals(2, filteredTodos.size());
	}
	
	@Test
	public void testRetrieveTodosRelatedToSpring_usingBMock_withEmptyClass() {
		
		TodoService todoServiceMock = mock(TodoService.class);
		
		// sth that acts as stub - we're defining what to do in the mock
		List<String> todos = Arrays.asList();
		when(todoServiceMock.retrieveTodos("Dummy")).thenReturn(todos);
		
		TodoBusinessImpl todoBusinessImpl = new TodoBusinessImpl(
				todoServiceMock);
		
		List<String> filteredTodos = todoBusinessImpl
				.retrieveTodosRelatedToSpring("Dummy");
		
		assertEquals(0, filteredTodos.size());
	}

}

// What is mocking?
// mocking is creating objects that simulate the behavior of real objects
// Unlike stubs, mocks can be dynamically created from code - at runtime
// Mocks offer more functionality than stubbing
// You can verify method calls and a lot of other things