package com.test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.ShutdownNotification;

public class Client {
    public static final String COMM_UP = null;
    static int SERVER_PORT = 3456;
    static int US_STREAM = 0;
    static int FR_STREAM = 1;
    static int DE_STREAM = 2;
    static int CH_STREAM = 3;
    static int file_size = 55000;
    static ByteBuffer list = ByteBuffer.allocateDirect(file_size);

    public static void main(String[] args) throws IOException {
        try{
            File file = new File("/home/michal/Desktop/antygona-new.txt");
            FileOutputStream fis = new FileOutputStream(file);

            InetSocketAddress serverAddr = new InetSocketAddress("localhost",
                    SERVER_PORT);
            ByteBuffer buf = ByteBuffer.allocateDirect(60);
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();

            SctpChannel sc = SctpChannel.open(serverAddr, 0, 0);

            /* handler to keep track of association setup and termination */
            AssociationHandler assocHandler = new AssociationHandler();

            /* expect two messages and two notifications */
            MessageInfo messageInfo = null;
            int counter = 0;
            int tokenSt = -1;
            int tokenEn = -1;
            do {
                messageInfo = sc.receive(buf, System.out, assocHandler);
                buf.flip();
                counter++;

                if (buf.remaining() > 0 && messageInfo.streamNumber() == US_STREAM) {
                    if(tokenSt == -1) {
                        tokenSt = US_STREAM;
                    } else {
                        if(tokenEn == -1) {
                            //start and end offset
                            counter -=2;
                            //list.put((byte)counter);
                            System.out.println("Decoded bits: "+(char)counter);
                            fis.write((char)counter);
                            counter=0;
                            tokenSt = -1;
                            tokenEn=-1;
                        }
                    }
                    //System.out.println("(US) " + decoder.decode(buf).toString());
                } else if (buf.remaining() > 0 && messageInfo.streamNumber() == FR_STREAM) {

                    //System.out.println("(FR) " +  decoder.decode(buf).toString());
                } else if(buf.remaining() > 0 && messageInfo.streamNumber() == DE_STREAM) {
                    //System.out.println("(DE) " +  decoder.decode(buf).toString());
                } else if(buf.remaining() > 0 && messageInfo.streamNumber() == CH_STREAM) {
                    if(tokenSt == -1) {
                        tokenSt = CH_STREAM;
                    } else {
                        if(tokenEn == -1) {
                            //start and end offset
                            counter -=2;
                            //list.put((byte)counter);
                            System.out.println("Decoded bits: "+(char)counter);
                            fis.write((char)counter);
                            counter=0;
                            tokenSt = -1;
                            tokenEn=-1;
                        }
                    }
                    //System.out.println("(CH) " +  decoder.decode(buf).toString());
                }
                buf.clear();
            } while (messageInfo != null);

            //todo why it is not displaying, messageinfo never is nulled
            System.out.println("Decoded message: "+list.get(0));

            sc.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    static class AssociationHandler extends AbstractNotificationHandler {
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {
            if (not.event().equals(COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
//                stream.printf("New association setup with %d outbound streams" +
//                        ", and %d inbound streams.\n", outbound, inbound);
            }

            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
            //stream.printf("The association has been shutdown.\n");
            return HandlerResult.RETURN;
        }
    }
}
