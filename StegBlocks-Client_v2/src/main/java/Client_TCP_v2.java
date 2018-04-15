import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Client_TCP_v2 implements Runnable {

    private static final String FILE_BEFORE = "/home/osboxes/Studia/BEST/projekt/StegBlocks/test.txt";
    private static final String FILE_AFTER = "/home/osboxes/Studia/BEST/projekt/StegBlocks/test-encoded.txt";
    private static final int START_PORT = 40000;    //could be changed later to spread ports across free space
    private static final int END_PORT = 40007;    //could be changed later to spread ports across free space
    private static final int CONNECTIONS_NUMBER = 6; //number of ports used to transmit characters
    private static final int FINISH_PORT = 4008;    //could be changed later to spread ports across free space
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String HOST = "10.0.2.15";

    private boolean transmission = false;
    private ByteBuffer filebuf;
    private List<PcapIf> alldevs; // Will be filled with NICs
    private StringBuilder errbuf; // For any error msgs
    private Pcap pcap; //sniffer
    private PcapPacketHandler<String> jpacketHandler; //packet handler
    private List<PcapPacket> tcpPacketPool;



    public Client_TCP_v2(boolean transmission) {
        this.transmission = transmission;
        tcpPacketPool = new ArrayList<>();
        this.alldevs = new ArrayList<PcapIf>();
        this.errbuf = new StringBuilder();
    }

    @Override
    public void run() {
        try {
            initializeFile();
            initializeSniffer();
            initializeHandler();
            if (transmission) {
                sendFile();
            } else {
                sendSomeTraffic();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendFile() throws UnknownHostException {
        logMessage("Start file transmission");
        filebuf.rewind();
        while (filebuf.hasRemaining()) {
            int sequence = filebuf.get();
            logMessage("Encoding: " + sequence);
            pcap.loop(sequence + 2, jpacketHandler, "jNetPcap rocks!"); //character number + 2(start and end sequence)
            sendCharacter(sequence);
        }
        endTransmission();
        logMessage("File transmission ended up");
    }

    private void sendCharacter(int sequence) throws UnknownHostException {
        sniffPackets(sequence + 2);
        sendPacket(tcpPacketPool.get(0), START_PORT);
        for (int i = 1; i <= sequence; i++) {
            sendPacket(tcpPacketPool.get(i),getPseudorandomPort());
        }
        sendPacket(tcpPacketPool.get(tcpPacketPool.size() - 1), END_PORT);
        tcpPacketPool.clear();
    }

    void sniffPackets(int number) {
        logMessage("Start sniffing " + number + " packets");
        pcap.loop(number, jpacketHandler, "jNetPcap rocks!"); //character number + 2(start and end sequence)
        while (true) {
            if (tcpPacketPool.size() < number) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                logMessage("Sniffing finished");
                break;
            }
        }
    }

    private int getPseudorandomPort() {
        return START_PORT + (int) (1 + (Math.random() * (CONNECTIONS_NUMBER)));
    }

    private void endTransmission() throws UnknownHostException {
        pcap.loop(1, jpacketHandler, "jNetPcap rocks!"); //get packet end
        sendPacket(tcpPacketPool.get(0), FINISH_PORT);
    }

    private void sendPacket(PcapPacket packetToOverwrite, int port) throws UnknownHostException {
        Ip4 ip = packetToOverwrite.getHeader(new Ip4());
        Tcp tcp = packetToOverwrite.getHeader(new Tcp());
        tcp.destination(port);
        ip.destination(Inet4Address.getByName(HOST).getAddress());
        //Recalculate packet checksum after overwrite
        ip.checksum(ip.calculateChecksum());
        tcp.checksum(tcp.calculateChecksum());
        if (pcap.sendPacket(packetToOverwrite) != Pcap.OK) {
            System.err.println(pcap.getErr() + " Packet size: " + packetToOverwrite.size());
        }
        System.out.println("Packet send size: " + packetToOverwrite.size());
    }

    private void sendSomeTraffic() {
        Timer timer = new Timer();
        //sending every 10ms current date
        timer.schedule( new TimerTask()
        {
            public void run() {
                pcap.loop(50, jpacketHandler, "jNetPcap rocks!");
                tcpPacketPool.forEach((packetToOverwrite) -> {
                    try {
                        sendPacket(packetToOverwrite, getPseudorandomPort());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 0, 500);
    }

    private void initializeHandler() {
        /***************************************************************************
         * Third we create a packet handler which will receive packets from the
         * libpcap loop.
         **************************************************************************/
        this.jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {
                Tcp tcp = new Tcp();
                /*
                    Save tcp packets
                 */
                if (packet.hasHeader(tcp)) {
                    tcpPacketPool.add(packet);
                }
            }
        };
    }

    private void initializeFile() throws IOException {
        new ParserServer(FILE_BEFORE, FILE_AFTER);
        readFile(FILE_AFTER);
    }

    private void initializeSniffer() {
        /***************************************************************************
         * First get a list of devices on this system
         **************************************************************************/
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf
                    .toString());
            return;
        }

        System.out.println("Network devices found:");

        int i = 0;
        for (PcapIf device : alldevs) {
            String description =
                    (device.getDescription() != null) ? device.getDescription()
                            : "No description available";
            System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
        }

        PcapIf device = alldevs.get(5); // We know we have atleast 1 device
        System.out
                .printf("\nChoosing '%s' on your behalf:\n",
                        (device.getDescription() != null) ? device.getDescription()
                                : device.getName());


        /*****************************************
         * Second we open a network interface
         *****************************************/
        int snaplen = 64 * 1024; // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        this.pcap =
                Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: "
                    + errbuf.toString());
            return;
        }
    }

    private void readFile(String path) throws IOException {
        System.out.println(path);
        File file = new File(path);
        filebuf = ByteBuffer.allocateDirect((int)file.length());
        InputStream is = new FileInputStream(file);
        for(int i=0;i<(int)file.length();i++) {
            filebuf.put((byte)is.read());
        }
        is.close();
    }

    private void logMessage(String message) {
        System.out.println("[" + LocalDateTime.now().format(format) + "] " + message);
    }

    public static void main(String[] args) {
        if(args[0].equals("true")) {
            new Client_TCP_v2(true).run();
        }
        else {
            new Client_TCP_v2(false).run();
        }
    }

}
