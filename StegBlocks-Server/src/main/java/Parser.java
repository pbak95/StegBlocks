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
	//on client side map the same, but withour polish signes on end
	static Map<Integer, Integer> codingMap = new HashMap<Integer, Integer>();
    static String sentence="";
    
    private static void setup() {
    	codingMap.put(32,0);//space
    	codingMap.put(1, 1);//a
    	codingMap.put(5,2); //e
    	codingMap.put(15,3);//o
    	codingMap.put(9,4);//i
    	codingMap.put(26,5);//z
    	codingMap.put(14,6);//n
    	codingMap.put(19,7);//s
    	codingMap.put(18,8);//r
    	codingMap.put(23,9);//w
    	codingMap.put(3,10);//c
    	codingMap.put(20,11);//t
    	codingMap.put(12, 12);//l
    	codingMap.put(25,13);//y
    	codingMap.put(11,14);//k
    	codingMap.put(4,15);//d
    	codingMap.put(16,16);//p
    	codingMap.put(13,17);//m
    	codingMap.put(21,18);//u
    	codingMap.put(10,19);//j
    	codingMap.put(2,20);//b
    	codingMap.put(7,21);//g
    	codingMap.put(8,22);//h
    	codingMap.put(6,23);//f
    	codingMap.put(16,24);//q
    	codingMap.put(24,25);//x
    	codingMap.put(22,26);//v
    	codingMap.put(44,27);//,
    	codingMap.put(46,28);//.
    	codingMap.put(63,29);//!
    	codingMap.put(33,30);//?
    	codingMap.put(261,1);//ą
    	codingMap.put(263,10);//ć
    	codingMap.put(281,2);//ę
    	codingMap.put(322,12);//ł
    	codingMap.put(243,3);//ó
    	codingMap.put(324,6);//ń
    	codingMap.put(347,7);//ś
    	codingMap.put(380,5);//ż
    	codingMap.put(378,5);//ż
	}

    public Parser(String file) throws IOException {
    	setup();
        FileInputStream fis = new FileInputStream(file);
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
		int ret = temp;
		try {
			ret = codingMap.get(temp);
			return ret;
		}
		catch(Exception ex){
			if(temp > 127) {
				return 0;
			}
			else {
				return temp;
			}
		}
	}
	
    //TO REMOVE and add on client side
	private static int decodeChar(int temp) {      
    	for (Entry<Integer, Integer> entry : codingMap.entrySet())
    	{
    	    if(entry.getValue() == temp) {
    	    	if(temp>=1 && temp <=26) {
    	        	return entry.getKey()+96;
    	        }
    	    	else {
    	    		return entry.getKey();	
    	    	}
    	    }
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