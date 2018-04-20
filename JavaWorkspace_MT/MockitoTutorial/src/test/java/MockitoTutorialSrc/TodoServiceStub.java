package MockitoTutorialSrc;

import java.util.Arrays;
import java.util.List;

public class TodoServiceStub implements TodoService{

	public List<String> retrieveTodos(String user) {
		// TODO Auto-generated method stub
		return Arrays.asList("Learn Spring MVC", "Learn Spring", "Learn to Dance");
	}

	public void setToDos(TodoService todoService) {
		// TODO Auto-generated method stub
		
	}

	public TodoService getToDos() {
		// TODO Auto-generated method stub
		return null;
	}

}

// Problems that arise with using Stubs:
// if there is dynamic behavior required
// if there is a huge logic that need to be implemented in a stub