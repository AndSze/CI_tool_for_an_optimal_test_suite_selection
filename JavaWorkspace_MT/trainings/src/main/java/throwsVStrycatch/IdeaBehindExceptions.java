package throwsVStrycatch;

public class IdeaBehindExceptions {

}

// exceptions require going back through the stack to find a exception handling code, thus they are very ineffective - use them when really needed

/*
readFile {
	try {
	    open the file;
	    determine its size;
	    allocate that much memory;
	    read the file into memory;
	    close the file;
	} catch (fileOpenFailed) {
	   doSomething;
	} catch (sizeDeterminationFailed) {
	    doSomething;
	} catch (memoryAllocationFailed) {
	    doSomething;
	} catch (readFailed) {
	    doSomething;
	} catch (fileCloseFailed) {
	    doSomething;
	}
	}
*/

/*
	method1 {
	try {
	    call method2;
	} catch (exception e) { // error processing only here 
	    doErrorProcessing;
	}
	}
	
	method2 throws exception {
	call method3;
	}
	
	method3 throws exception {
	call readFile;
	}
*/

/*
determine how specific your error handler is

	1) catch (IOException e) {
	...
	}
	This handler will be able to catch all I/O exceptions, including FileNotFoundException, EOFException, and so on

	2) catch (FileNotFoundException e) {
	...
	}
	
	The FileNotFoundException class has no descendants, so the following handler can handle only one type of exception.
	
	You could even set up an exception handler that handles any Exception with the handler here.

	3) catch (Exception e) { // A (too) general exception handler
	    ... 
	}

	The Exception class is close to the top of the Throwable class hierarchy. Thus, this handler will catch many other exceptions in addition to those
	that the handler is intended to catch. 
	You may want to handle exceptions this way if all you want your program to do, for example, is print out an error message for the user and then exit.
	
*/