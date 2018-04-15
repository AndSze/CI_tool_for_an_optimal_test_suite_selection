package throwsVStrycatch;

import java.io.IOException;

// checked exception - has to be handled by programmer (eg. IOException, EOFException, FileNotFoundException) 
// each checked exception inherits from Exception, but does not contain RuntimeException in its inheritance hierarchy
// checked exceptions should be applied in case a program can fix an exception occurrence 

// unchecked exception - compiler handles it (eg. IllegalArgumentException, NullPointerException, IndexOutOfBoundsException, NumberFormatException)
// an unchecked exception is each exception that is not a checked exception
// handling exceptions of this type by a programmer will be difficult since they can happen in many lines of code

public class CheckedExceptions {
    public static void main(String[] args) {
        CheckedExceptions instance = new CheckedExceptions();
        try {
        	int hours = -3;
        	int numberOfSeconds = 0;
        	try {
        	    numberOfSeconds = instance.getNumberOfSeconds(hours);
        	}
        	catch (ArithmeticException | IllegalArgumentException exception) {
        	    numberOfSeconds = instance.getNumberOfSeconds(hours * -1);
        	}
        	System.out.println(numberOfSeconds);

            instance.methodWithCheckedException();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    private void methodWithCheckedException() throws IOException {
        throw new IOException();
    }
    
    public int getNumberOfSeconds(int hour) {
        if (hour < 0) {
            throw new IllegalArgumentException("Hour must be >= 0: " + hour);
        }
        return hour * 60 * 60;
    }

}

/*
1) Pierwsza i najwa�niejsza zasada, blok try powinien by� jak najmniejszy. Takie podej�cie bardzo u�atwia znajdowanie b��d�w w
	bardziej skomplikowanych programach. Dzi�ki ma�emu blokowi try tak�e mo�emy napisa� lepszy kod do obs�ugi wyj�tku � 
	wiemy dok�adnie z kt�rego miejsca wyj�tek mo�e zosta� rzucony wi�c wiemy tak�e jak najlepiej na niego zareagowa�.
2) Blok finally bardzo cz�sto jest niezb�dny. Szczeg�lnie je�li operujemy na instancjach, kt�re wymagaj� �zamkni�cia�.
3) U�ywaj klas wyj�tk�w, kt�re idealnie pasuj� do danej sytuacji. Je�li nie ma takiego wyj�tku w bibliotece standardowej utw�rz w�asn� klas� wyj�tku.
4) Tworz�c instancj� wyj�tk�w podawaj mo�liwie najdok�adniejszy opis w tre�ci wyj�tku. Pozwala to na du�o �atwiejsze 
	znajdowanie b��d�w w programie je�li komunikat wyj�tku jest szczeg�owy.
5) Nie zapominaj o u�ywaniu wyj�tk�w typu checked. Chocia� wymagaj� troch� wi�cej kodu i generuj� cz�sto irytuj�ce b��dy
	kompilacji ich u�ywanie jest czasami wskazane.
*/
