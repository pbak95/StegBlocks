import java.io.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors.*;
import java.util.stream.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class Parser {
	static Map<Integer, Integer> codingMap = new HashMap<Integer, Integer>();
    static String sentence="";
    
    private static void setup() {
    	codingMap.put(0, 32);//space
    	codingMap.put(1, 1);//a
    	codingMap.put(2, 5); //e
    	codingMap.put(3, 15);//o
    	codingMap.put(4, 9);//i
    	codingMap.put(5, 26);//z
    	codingMap.put(6, 14);//n
    	codingMap.put(7, 19);//s
    	codingMap.put(8, 18);//r
    	codingMap.put(9, 23);//w
    	codingMap.put(10, 3);//c
    	codingMap.put(11, 20);//t
    	codingMap.put(12, 12);//l
    	codingMap.put(13, 25);//y
    	codingMap.put(14, 11);//k
    	codingMap.put(15, 4);//d
    	codingMap.put(16, 16);//p
    	codingMap.put(17, 13);//m
    	codingMap.put(18, 21);//u
    	codingMap.put(19, 10);//j
    	codingMap.put(20, 2);//b
    	codingMap.put(21, 7);//g
    	codingMap.put(22, 8);//h
    	codingMap.put(23, 6);//f
    	codingMap.put(24, 16);//q
    	codingMap.put(25, 24);//x
    	codingMap.put(26, 22);//v
    	codingMap.put(27, 44);//,
    	codingMap.put(28, 46);//.
    	codingMap.put(29, 63);//!
    	codingMap.put(30, 33);//?
    	codingMap.put(31, 261);//ą
    	codingMap.put(32, 263);//ć
    	codingMap.put(33, 281);//ę
    	codingMap.put(34, 322);//ł
    	codingMap.put(35, 243);//ó
    	codingMap.put(36, 324);//ń
    	codingMap.put(37, 347);//ś
    	codingMap.put(38, 380);//ż
    	codingMap.put(39, 378);//ż
	}
    
    public static void main(String[] args) throws IOException {
    	setup();
        FileInputStream fis = new FileInputStream("antygona.txt");
        InputStreamReader fr = new InputStreamReader(fis, "Cp1250");
        BufferedReader br = new BufferedReader(fr);
        String sz = null;
        while((sz=br.readLine())!=null){
            sentence = sentence.concat(sz.toLowerCase());
            sentence = sentence.concat(" ");
        }
        br.close();
        
        int temp = 0;
        for(int i=0;i<sentence.length();i++) {
            int temp2 = sentence.charAt(i);
            temp+=temp2;
        }
        System.out.println("Tyle znakow przed: "+temp);
//        HashMap<String,Integer> unsortedMap = new HashMap<String,Integer>();
//
//        Stream<String> wordStream = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS).splitAsStream(sentence);
//
//        wordStream.forEach((wordReal) -> {
//            String word = wordReal.toLowerCase(); //tu robimy lowercase to nam trochê zmienia ale u³atwia
//            if (!word.equals("")) {
//                if (unsortedMap.get(word) == null) {
//                    unsortedMap.put(word, 0);
//                }
//                unsortedMap.put(word, unsortedMap.get(word) + 1);
//            }
//        });
//
//        // sort hashmap after value desc
//        Map<String, Integer> sortedMap =
//                unsortedMap.entrySet().stream()
//                        .sorted(Map.Entry.comparingByValue((v1,v2)->v2.compareTo(v1)))
//                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
//                                (e1, e2) -> e1, LinkedHashMap::new));
//
//        int counter = 0;
//        int counter2 = 0;
//        ArrayList<String> mostcommon = new ArrayList<String>();
//        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
//            if(entry.getKey().length() >= 5) {
//                mostcommon.add(entry.getKey());
//                counter++;
//                System.out.println("Word : `" + entry.getKey() + "` Count : " + entry.getValue());
//                int ctr = 0;
//                for(int i=0;i<entry.getKey().length();i++) {
//                    int temp2 = entry.getKey().charAt(i);
//                    ctr+=temp2;
//                }
//                ctr*=entry.getValue();
//                counter2+=ctr;
//                if(counter >=15) break;
//            }
//        }
//            
//        System.out.println("Tyle \"waza\" "+counter+" most common slowa: "+(counter2));
        reaarrangeFile();
    }
    
	private static void reaarrangeFile() throws IOException {
        ArrayList<Byte> byteArray = new ArrayList();
        for(int i=0;i<sentence.length();i++) {
            int temp = sentence.charAt(i);

            //kodowanie liter
            if(temp>=97 && temp <=122) {
            	temp-=96;
                temp=codeChar(temp);
            }
            else {
            	temp=codeChar(temp);
            }
            byteArray.add((byte) temp);
        }

        int temp3 = 0;
        for(int i=0;i<byteArray.size();i++) {
            int temp2 = byteArray.get(i);
            if(temp2 >= 0) {
                temp3+=temp2;
            }
        }
        System.out.println("Tyle znakow po ostateczenej kompresji: "+temp3);

        saveToFile(byteArray);
        
        //TO REMOVE and add on client side
        String m = decode();
        System.out.println("After decoding: Length: "+m.length()+" Text: "+m.substring(0, 200));
    }
	
    //TO REMOVE and add on client side
    private static String decode() throws IOException {
		String message = "";
		FileInputStream fis = new FileInputStream("antygona-encoded.txt");
        InputStreamReader fr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(fr);
        int sz = -1;
        while((sz=br.read())!=-1){
        	message = message.concat(Character.toString((char) decodeChar(sz)));
        }
		return message;
	}
    
	private static void saveToFile(ArrayList<Byte> byteArray) throws IOException {
        FileOutputStream outputStream = new FileOutputStream("antygona-encoded.txt");
        byte[] strToBytes = new byte[byteArray.size()];
        for (int i = 0; i < byteArray.size(); i++) {
            strToBytes[i] = byteArray.get(i);
        }
        outputStream.write(strToBytes);
        outputStream.close();
	}
    
	private static int codeChar(int temp) {
    	for (Entry<Integer, Integer> entry : codingMap.entrySet())
    	{
    	    if(entry.getValue() == temp) {
    	    	return entry.getKey();
    	    }
    	}
    	return temp;
	}
	
    //TO REMOVE and add on client side
	private static int decodeChar(int temp) {
        //kodowanie liter
        if(temp>=1 && temp <=26) {
        	temp = codingMap.get(temp)+96;
        }
        //kodowanie przecinka,kropki,wykrzyknika i pytajnika
        else if(temp < 40) {
        	temp = codingMap.get(temp);
        }
    	return temp;
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