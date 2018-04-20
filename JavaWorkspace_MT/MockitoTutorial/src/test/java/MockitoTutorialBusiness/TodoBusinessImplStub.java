package MockitoTutorialBusiness;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import MockitoTutorialSrc.TodoService;
import MockitoTutorialSrc.TodoServiceStub;

public class TodoBusinessImplStub {
	
	TodoService todoServiceStub;
	TodoBusinessImpl todoBusinessImpl;

	@Before
	public void setUp() throws Exception {
		todoServiceStub = new TodoServiceStub();
		todoBusinessImpl = new TodoBusinessImpl();
	} 

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRetrieveTodosRelatedToSpring_usingAStub() {
	
		todoBusinessImpl.setToDos(todoServiceStub);
		List<String> filteredTodos = todoBusinessImpl
				.retrieveTodosRelatedToSpring("Dummy");
		
		assertEquals(2, filteredTodos.size());
	}
	
	@Test
	public void testRetrieveTodosRelatedToSpring_usingBStub() {
		todoBusinessImpl.setToDos(todoServiceStub);
		
		List<String> filteredTodos = todoBusinessImpl
				.retrieveTodosRelatedToSpring("Dummy");
			
		assertEquals("Learn Spring MVC", filteredTodos.get(0));
	}

}
