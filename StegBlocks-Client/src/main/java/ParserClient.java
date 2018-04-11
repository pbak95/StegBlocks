package main.java;

import java.io.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors.*;
import java.util.stream.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class ParserClient {
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
    }

    public ParserClient(String file) throws IOException {
        setup();
        String m = decode(file);
        System.out.println("After decoding: Length: "+m.length()+" Text: "+m);
        saveToFile(m);
    }

    private static String decode(String file) throws IOException {
        String message = "";
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader fr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(fr);
        int sz = -1;
        while((sz=br.read())!=-1){
            message = message.concat(Character.toString((char) decodeChar(sz)));
        }
        return message;
    }

    private static void saveToFile(String message) throws IOException {
        PrintWriter out = new PrintWriter("antygona-decoded.txt");
        out.println(message);
        out.close();
    }

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