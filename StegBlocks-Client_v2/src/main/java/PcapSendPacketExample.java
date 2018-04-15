import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.sun.org.apache.xerces.internal.util.IntStack;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.JMemoryPacket;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.JProtocol;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

public class PcapSendPacketExample {
    public static void main(String[] args) throws UnknownHostException {
        List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
        StringBuilder errbuf = new StringBuilder(); // For any error msgs
        final List<PcapPacket> tcpPacket = new ArrayList<>();

        /***************************************************************************
         * First get a list of devices on this system
         **************************************************************************/
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
            return;
        }
        PcapIf device = alldevs.get(5); // We know we have atleast 1 device

        /*****************************************
         * Second we open a network interface
         *****************************************/
        int snaplen = 64 * 1024; // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        Pcap pcap = Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);


        /*******************************************************
         * Third we create our crude packet we will transmit out
         * This creates a broadcast packet
         *******************************************************/


//        JPacket packet =
//                new JMemoryPacket(JProtocol.ETHERNET_ID,
//                        " 001801bf 6adc0025 4bb7afec 08004500 "
//                                + " 0041a983 40004006 d69ac0a8 00342f8c "
//                                + " ca30c3ef 008f2e80 11f52ea8 4b578018 "
//                                + " ffffa6ea 00000101 080a152e ef03002a "
//                                + " 2c943538 322e3430 204e4f4f 500d0a");
//
//        Ip4 ip = packet.getHeader(new Ip4());
//        Tcp tcp = packet.getHeader(new Tcp());
//        ip.destination(Inet4Address.getByName("10.0.2.15").getAddress());
//        ip.source(Inet4Address.getByName("10.0.2.5").getAddress());
//
//        //tcp.destination(80);
//
//        ip.checksum(ip.calculateChecksum());
//        tcp.checksum(tcp.calculateChecksum());
//        packet.scan(Ethernet.ID);
//
//        System.out.println(packet);
//
//        /*******************************************************
//         * Fourth We send our packet off using open device
//         *******************************************************/
//        for (int i = 0; i < 10; i++) {
//            if (pcap.sendPacket(packet) != Pcap.OK) {
//                System.err.println(pcap.getErr());
//            }
//        }

        PcapBpfProgram program = new PcapBpfProgram();
        String expression = "host 10.0.2.15";
        int optimize = 0;         // 0 = false
        int netmask = 0xFFFFFF00; // 255.255.255.0

        if (pcap.compile(program, expression, optimize, netmask) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }

        if (pcap.setFilter(program) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }

        /***************************************************************************
         * Third we create a packet handler which will receive packets from the
         * libpcap loop.
         **************************************************************************/
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {

                Ip4 ip = new Ip4(); // Preallocat IP version 4 header
                Tcp tcp = new Tcp();

                if (packet.hasHeader(ip)) {
                    System.out.printf("ip.version=%d\n", ip.version());
                    System.out.println("ip.dst=" + FormatUtils.ip(ip.destination()));
                }
                if (packet.hasHeader(tcp)) {
                    System.out.printf("tcp.dstPort=%d\n", tcp.destination());
                    System.out.printf("tcp.srcPort=%d\n", tcp.source());
                    tcpPacket.add(packet);
                }


                System.out.printf("Received packet at %s caplen=%-4d len=%-4d %s\n",
                        new Date(packet.getCaptureHeader().timestampInMillis()),
                        packet.getCaptureHeader().caplen(),  // Length actually captured
                        packet.getCaptureHeader().wirelen(), // Original length
                        user                                 // User supplied object
                );
            }
        };

        /***************************************************************************
         * Fourth we enter the loop and tell it to capture 10 packets. The loop
         * method does a mapping of pcap.datalink() DLT value to JProtocol ID, which
         * is needed by JScanner. The scanner scans the packet buffer and decodes
         * the headers. The mapping is done automatically, although a variation on
         * the loop method exists that allows the programmer to sepecify exactly
         * which protocol ID to use as the data link type for this pcap interface.
         **************************************************************************/
        pcap.loop(10, jpacketHandler, "jNetPcap rocks!");

//        PcapPacket packetToSend = tcpPacket.get(0);
//        System.out.println(packetToSend);
//        Ip4 ip = packetToSend.getHeader(new Ip4());
//        Tcp tcp = packetToSend.getHeader(new Tcp());
//        tcp.destination(40000);
//        ip.destination(Inet4Address.getByName("10.0.2.15").getAddress());
//        //ip.source(Inet4Address.getByName("127.0.0.1").getAddress());
//        ip.checksum(ip.calculateChecksum());
//        tcp.checksum(tcp.calculateChecksum());
//
//        System.out.println("///////////////////////////////////////////////////////");
//
//
//        System.out.println(packetToSend);


        for (int i = 0; i < 100; i++) {
            PcapPacket packetToSend = tcpPacket.get((int) (1 + (Math.random() * (tcpPacket.size() -1 ))));
            System.out.println(packetToSend);
            Ip4 ip = packetToSend.getHeader(new Ip4());
            Tcp tcp = packetToSend.getHeader(new Tcp());
            tcp.destination(40000);
            ip.destination(Inet4Address.getByName("10.0.2.15").getAddress());
            //ip.source(Inet4Address.getByName("127.0.0.1").getAddress());
            ip.checksum(ip.calculateChecksum());
            tcp.checksum(tcp.calculateChecksum());
            if (pcap.sendPacket(packetToSend) != Pcap.OK) {
                System.err.println(pcap.getErr());
            }
        }


        /********************************************************
         * Lastly we close
         ********************************************************/
        pcap.close();
    }
}  