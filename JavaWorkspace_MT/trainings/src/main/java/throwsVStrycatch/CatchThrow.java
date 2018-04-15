package throwsVStrycatch;
/*
1) The try block will execute a sensitive code which can throw exceptions
2) The catch block will be used whenever an exception (of the type caught) is thrown in the try block
3) The finally block is called in every case after the try/catch blocks. Even if the exception isn't caught or if your previous blocks break the execution flow.
4) The throw keyword will allow you to throw an exception (which will break the execution flow and can be caught in a catch block).
5) The throws keyword in the method prototype is used to specify that your method might throw exceptions of the specified type. 
	It's useful when you have checked exception (exception that you have to handle) that you don't want to catch in your current method.
*/
public class CatchThrow {

private static void throwsMethod() throws NumberFormatException {
    String  intNumber = "5A";

    Integer.parseInt(intNumber);
}

private static void catchMethod() {
    try {

        throwsMethod();

    } catch (NumberFormatException e) {
        System.out.println("Convertion Error");
    }

}

public static void main(String[] args) {
    // TODO Auto-generated method stub

    catchMethod();
}

}
