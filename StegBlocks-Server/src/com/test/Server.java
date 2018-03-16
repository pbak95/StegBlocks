package com.test;

import java.io.*;
import java.net.InetSocketAddress;
       import java.nio.ByteBuffer;
       import java.nio.CharBuffer;
        import java.nio.charset.Charset;
        import java.nio.charset.CharsetEncoder;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Locale;

        import com.sun.nio.sctp.MessageInfo;
        import com.sun.nio.sctp.SctpChannel;
        import com.sun.nio.sctp.SctpServerChannel;
import jdk.internal.util.xml.impl.Input;

public class Server {
    static int SERVER_PORT = 3456;
    static int US_STREAM = 0;
    static int FR_STREAM = 1;
    static int DE_STREAM = 2;
    static int CH_STREAM = 3;
    static int stream_size = 4;

    static SimpleDateFormat USformatter = new SimpleDateFormat(
            "h:mm:ss a EEE d MMM yy, zzzz", Locale.US);
    static SimpleDateFormat FRformatter = new SimpleDateFormat(
            "h:mm:ss a EEE d MMM yy, zzzz", Locale.FRENCH);
    static SimpleDateFormat DEformatter = new SimpleDateFormat(
            "h:mm:ss a EEE d MMM yy, zzzz", Locale.GERMAN);
    static SimpleDateFormat CHformatter = new SimpleDateFormat(
            "h:mm:ss a EEE d MMM yy, zzzz", Locale.CHINESE);

    static ByteBuffer buf = ByteBuffer.allocateDirect(60);
    static CharBuffer cbuf = CharBuffer.allocate(60);
    static Charset charset = Charset.forName("ISO-8859-1");
    static CharsetEncoder encoder = charset.newEncoder();

    static ByteBuffer filebuf;

    static SctpChannel sc;

    public static void main(String[] args) throws IOException {

        File antygona = new File("/home/michal/Desktop/antygona.txt");
        filebuf = ByteBuffer.allocateDirect((int)antygona.length());
        InputStream is = new FileInputStream(antygona);
        for(int i=0;i<(int)antygona.length();i++) {
            filebuf.put((byte)is.read());
        }
        is.close();

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);

        while (true) {
            sc = ssc.accept();

            /* get the current date */
            Date today = new Date();

            //send today date in multilanguage and hide message
            filebuf.rewind();
            while(filebuf.hasRemaining()) {
                int temp = filebuf.get();
                System.out.println("Encoding: "+temp);
                sendDate(today,temp);
            }
//            sendDate(today,0);
//            sendDate(today,1);
//            sendDate(today,4);
//            sendDate(today,8);
//            sendDate(today,15);
//            sendDate(today,46);
//            sendDate(today,120);
//            sendDate(today,121);
//            sendDate(today,122);
//            sendDate(today,245);

            sc.close();
        }
    }

    private static void sendDate(Date today, int seq) throws IOException {
        //send first token
        sendStream(today,0);

        //todo smth more complex
        for(int i=0;i<seq;i++) {
            int streamno = (int) (1 + (Math.random() * (stream_size-2)));
            sendStream(today,streamno);
        }

        //send end token
        sendStream(today,3);
    }

    private static void sendStream(Date today, int i) throws IOException {
        MessageInfo messageInfo = MessageInfo.createOutgoing(null,
                US_STREAM);
        cbuf.clear();
        buf.clear();
        if(i==0){
            cbuf.put(USformatter.format(today)).flip();
            encoder.encode(cbuf, buf, true);
            buf.flip();
            messageInfo.streamNumber(US_STREAM);
            sc.send(buf, messageInfo);
        } else if(i==1) {
            cbuf.put(FRformatter.format(today)).flip();
            encoder.encode(cbuf, buf, true);
            buf.flip();
            messageInfo.streamNumber(FR_STREAM);
            sc.send(buf, messageInfo);
        } else if(i==2) {
            cbuf.put(DEformatter.format(today)).flip();
            encoder.encode(cbuf, buf, true);
            buf.flip();
            messageInfo.streamNumber(DE_STREAM);
            sc.send(buf, messageInfo);
        } else if(i==3) {
            cbuf.put(CHformatter.format(today)).flip();
            encoder.encode(cbuf, buf, true);
            buf.flip();
            messageInfo.streamNumber(CH_STREAM);
            sc.send(buf, messageInfo);
        }
    }
}
