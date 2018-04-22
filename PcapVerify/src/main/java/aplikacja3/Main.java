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
    private static int startPort = 1022;
    private static int endPort = 1500;
    private static int finishPort = 68;

    public static void main(String[] args) throws IOException {

        final Pcap pcap = Pcap.openStream("..\\withencoding.pcap");
        streams = new ArrayList();

        pcap.loop(new PacketHandler() {

            public boolean nextPacket(Packet packet) throws IOException {

                if (packet.hasProtocol(Protocol.TCP)) {

                    TCPPacket tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);
                    Buffer buffer = tcpPacket.getPayload();
                    int port = tcpPacket.getDestinationPort();
                    if (buffer != null) {
                        if (port == startPort && !progress) {
                            //znacznik pierwszy
                            progress = true;
                        } else if (port == endPort && progress) {
                            //znacznik końcowy
                            progress = false;
                            if (counter > max) {
                                max = counter;
                            }
                            counter = 0;
                        } else if(port == finishPort && !progress) {
                            return true;
                        }
                        else {
                            counter++;
                        }
                    }
                }
                return true;
            }
        });
        if (max > 10) {
            System.out.println("It contains secret message.");
        } else {
            System.out.println("It does not contain secret message.");
        }
    }
}