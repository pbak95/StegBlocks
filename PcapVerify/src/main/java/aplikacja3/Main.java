package aplikacja3;

import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.protocol.Protocol;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    private static ArrayList<Integer> streams;
    private static int counter = 0;
    private static int max = 0;
    private static boolean progress = false;

    public static void main(String[] args) throws IOException {

        final Pcap pcap = Pcap.openStream("..\\withencoding.pcap");
        streams = new ArrayList();

        pcap.loop(new PacketHandler() {
            @Override
            public boolean nextPacket(Packet packet) throws IOException {

                if (packet.hasProtocol(Protocol.TCP)) {

                    TCPPacket tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);
                    if(tcpPacket.isPSH()) {
                        Buffer buffer = tcpPacket.getPayload();
                        int port = tcpPacket.getDestinationPort();
                        if (buffer != null) {
                            if(port == streams.get(0) && !progress) {
                                //znacznik pierwszy
                                progress = true;
                            }
                            else if(port == streams.get(streams.size()-1) && progress) {
                                //znacznik końcowy
                                progress = false;
                                if(counter > max) {
                                    max = counter;
                                }
                                counter = 0;
                            }
                            else {
                                counter++;
                            }
                        }
                    }
                    else if(tcpPacket.isSYN() && !tcpPacket.isACK()) {
                        //System.out.println(tcpPacket.getSourcePort());
                        streams.add(tcpPacket.getSourcePort());
                    }
                }
                return true;
            }
        });
        if(max > 10) {
            System.out.println("Zawiera ukryte dane");
        }
        else {
            System.out.println("Nie zawiera ukrytych danych");
        }
    }
}