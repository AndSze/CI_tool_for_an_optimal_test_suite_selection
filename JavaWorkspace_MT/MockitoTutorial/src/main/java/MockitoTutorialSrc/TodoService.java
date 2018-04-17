package MockitoTutorialSrc;

import java.util.List;

public interface TodoService {
	
	// interface attribute
	public List<String> retrieveTodos(String user);
	public void setToDos(TodoService todoService);
	public TodoService getToDos();
}
