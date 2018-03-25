import java.util.regex.Pattern;
import java.util.stream.Collectors.*;
import java.util.stream.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

enum Coding {
	SPACE,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,COMMA,DOT,QUE,EX
	//potem mostcommon, cyfry,reszta znaków specjalnych
}

public class Parser {

    public static void main(String[] args) throws IOException {
        FileReader fr = new FileReader("antygona.txt");
        BufferedReader br = new BufferedReader(fr);
        String sentence = "";
        String sz = null;
        while((sz=br.readLine())!=null){
        	sentence = sentence.concat(sz);
        	sentence = sentence.concat(" ");
        }
    	
        System.out.println("Tyle znakow: "+sentence.length());
        HashMap<String,Integer> unsortedMap = new HashMap<String,Integer>();
        
        Stream<String> wordStream = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS).splitAsStream(sentence);
        // foreach word count how many the word occurs in the wordstream
        wordStream.forEach((wordReal) -> {
            String word = wordReal.toLowerCase(); //tu robimy lowercase to nam trochê zmienia ale u³atwia 
            if (!word.equals("")) {
                if (unsortedMap.get(word) == null) {
                    unsortedMap.put(word, 0);
                }
                unsortedMap.put(word, unsortedMap.get(word) + 1);
            }
        });

        // sort hashmap after value desc
        Map<String, Integer> sortedMap =
             unsortedMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((v1,v2)->v2.compareTo(v1)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                                      (e1, e2) -> e1, LinkedHashMap::new));
        
        int counter = 0;
        int counter2 = 0;
        ArrayList<String> mostcommon = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            if(entry.getKey().length() > 1) {
            	mostcommon.add(entry.getKey());
                counter++;
                System.out.println("Word : `" + entry.getKey() + "` Count : " + entry.getValue());
                counter2+=entry.getValue()*entry.getKey().length();
                counter2--;
                if(counter >=20) break;
            }
        }
        System.out.println("Tyle znakow po kompresji: "+(sentence.length()-counter2));
        reaarrangeFile(mostcommon);
    }
    private static void reaarrangeFile(ArrayList<String> mostcommon) {
        //TODO wszystkie s³owa z mapy mostcommon zamieniæ na kodowanie + ca³y plik
	}
	public static byte[] readBytes(File file) {
        FileInputStream fis = null;
        byte[] b = null;
        try {
            fis = new FileInputStream(file);
            b = readBytesFromStream(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(fis);
        }
        return b;
    }
    public static byte[] readBytesFromStream(InputStream readStream) throws IOException {
        ByteArrayOutputStream writeStream = null;
        byte[] byteArr = null;
        writeStream = new ByteArrayOutputStream();
        try {
            copy(readStream, writeStream);
            writeStream.flush();
            byteArr = writeStream.toByteArray();
        } finally {
            close(writeStream);
        }
        return byteArr;
    }

public static void close(InputStream inStream) {
    try {
        inStream.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    inStream = null;
}
public static long copy(InputStream readStream, OutputStream writeStream) throws IOException {
    int bytesread = -1;
    byte[] b = new byte[4096]; //4096 is default cluster size in Windows for < 2TB NTFS partitions
    long count = 0;
    bytesread = readStream.read(b);
    while (bytesread != -1) {
        writeStream.write(b, 0, bytesread);
        count += bytesread;
        bytesread = readStream.read(b);
    }
    return count;
}
public static void close(OutputStream outStream) {
    try {
        outStream.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    outStream = null;
}
}