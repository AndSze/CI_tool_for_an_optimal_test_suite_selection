package MockitoTutorialBusiness;

import java.util.ArrayList;
import java.util.List;
import MockitoTutorialSrc.TodoService;

// TodoBusinessImpl - it is SUT (System under Test)
// TodoService - it is dependency

public class TodoBusinessImpl {
	
	// class attribute
	private TodoService todoService;

	public TodoBusinessImpl() {
	}
	
	// overwrite
	public void setToDos(TodoService todoService) {
		this.todoService = todoService;
	}
	
	// overwrite
	public TodoService getToDos() {
		return this.todoService;
	}
	
	// class public methods
	public List<String> retrieveTodosRelatedToSpring(String user){
		List<String> filteredTodos = new ArrayList<String>();
		List<String> todos = todoService.retrieveTodos(user);
		for(String todo:todos) {
			if(todo.contains("Spring")) {
				filteredTodos.add(todo);
			}
		}
		return filteredTodos;
	
	}

}
