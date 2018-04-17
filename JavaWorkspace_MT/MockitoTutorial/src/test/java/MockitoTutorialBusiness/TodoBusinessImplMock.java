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

	//Dependencies
	private TodoService todoServiceMock;
	
	//Class to be tested
	private TodoBusinessImpl todoBusinessImpl;
	
	// If you have multiple test methods, it makes sense to move the mock creation process to a single place and only differentiate its behavior for each individual test. 
	@Before
	public void setUp() throws Exception {
		// Comparison between Stubs and Mocks
		//TodoService mockTodoService = mock(TodoService.class);
		//stub(mockTodoService.retrieveTodos("Parameter")).return("value");
		todoBusinessImpl = new TodoBusinessImpl();
		todoServiceMock = mock(TodoService.class);
		todoBusinessImpl.setToDos(todoServiceMock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRetrieveTodosRelatedToSpring_usingAMock() {
		
		// sth that acts as stub - we're defining what to do in the mock
		List<String> todos = Arrays.asList("Learn Spring MVC", "Learn Spring", "Learn to Dance");
		
		// this line determines what will happen when somebody calls the retrieveTodos() method on the todoServiceMock
		when(todoServiceMock.retrieveTodos("Dummy")).thenReturn(todos);

		// when =~ stub
		
		// We create our class under test and pass to it our own mocked todoServiceMock as a dependency
		/*TodoBusinessImpl todoBusinessImpl = new TodoBusinessImpl(
		todoServiceMock);*/
		
		// Now our class under test does not know that the todoServiceMock is fake
		List<String> filteredTodos = todoBusinessImpl
				.retrieveTodosRelatedToSpring("Dummy");
		
		assertEquals(2, filteredTodos.size());
	}
	
	@Test
	public void testRetrieveTodosRelatedToSpring_usingBMock_withEmptyClass() {
		
		// sth that acts as stub - we're defining what to do in the mock
		List<String> todos = Arrays.asList();
		when(todoServiceMock.retrieveTodos("Dummy")).thenReturn(todos);

		/*TodoBusinessImpl todoBusinessImpl = new TodoBusinessImpl(
				todoServiceMock);*/
		
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



