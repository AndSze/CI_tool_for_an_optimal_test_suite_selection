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
1) Pierwsza i najwa¿niejsza zasada, blok try powinien byæ jak najmniejszy. Takie podejœcie bardzo u³atwia znajdowanie b³êdów w
	bardziej skomplikowanych programach. Dziêki ma³emu blokowi try tak¿e mo¿emy napisaæ lepszy kod do obs³ugi wyj¹tku – 
	wiemy dok³adnie z którego miejsca wyj¹tek mo¿e zostaæ rzucony wiêc wiemy tak¿e jak najlepiej na niego zareagowaæ.
2) Blok finally bardzo czêsto jest niezbêdny. Szczególnie jeœli operujemy na instancjach, które wymagaj¹ “zamkniêcia”.
3) U¿ywaj klas wyj¹tków, które idealnie pasuj¹ do danej sytuacji. Jeœli nie ma takiego wyj¹tku w bibliotece standardowej utwórz w³asn¹ klasê wyj¹tku.
4) Tworz¹c instancjê wyj¹tków podawaj mo¿liwie najdok³adniejszy opis w treœci wyj¹tku. Pozwala to na du¿o ³atwiejsze 
	znajdowanie b³êdów w programie jeœli komunikat wyj¹tku jest szczegó³owy.
5) Nie zapominaj o u¿ywaniu wyj¹tków typu checked. Chocia¿ wymagaj¹ trochê wiêcej kodu i generuj¹ czêsto irytuj¹ce b³êdy
	kompilacji ich u¿ywanie jest czasami wskazane.
*/
