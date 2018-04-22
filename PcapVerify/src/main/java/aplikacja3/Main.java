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
    private static int counter1 = 0;
    private static int counter2 = 0;
    private static int max = 0;
    private static boolean progress = false;
    private static int startPort = 122;
    private static int endPort = 150;
    private static int finishPort = 68;

    public static void main(String[] args) throws IOException {

        final Pcap pcap = Pcap.openStream(args[0]);
        streams = new ArrayList();

        pcap.loop(new PacketHandler() {

            public boolean nextPacket(Packet packet) throws IOException {

                if (packet.hasProtocol(Protocol.TCP)) {

                    TCPPacket tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);
                    int port = tcpPacket.getDestinationPort();
                    if (port == startPort) {
                        counter1++;
                    } else if (port == endPort) {
                        counter2++;
                    }
                }
                return true;
            }
        });
        if (counter1 > 10 && counter2 > 10) {
            System.out.println("It contains secret message.");
        } else {
            System.out.println("It does not contain secret message.");
        }
    }
}