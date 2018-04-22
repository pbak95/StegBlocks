import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Server_TCP_v2 implements Runnable {

    private static String PATH = "/home/osboxes/Studia/BEST/projekt/StegBlocks/antygona-new.txt";
    private static final int PORTS_NUMBER = 10;
    private static final int START_PORT = 122;    //could be changed later to spread ports across free space
    private static final int END_PORT = 150;    //could be changed later to spread ports across free space
    private static final int FINISH_PORT = 68;    //could be changed later to spread ports across free space
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String FILTER = "host 10.0.2.15"; //port filter doesn't work, so I filter by host

    private static volatile int CHARACTER_COUNTER = 0;
    public List<Integer> characterList;
    private static ArrayList<Integer> portmap;

    private Map<Integer, ServerSocket> connectionsPool;
    private List<PcapIf> alldevs; // Will be filled with NICs
    private StringBuilder errbuf; // For any error msgs
    private Pcap pcap; //sniffer
    private PcapPacketHandler<String> jpacketHandler; //packet handler

    private File file;
    private FileOutputStream fileOutputStream;


    public Server_TCP_v2() {
        this.connectionsPool = new HashMap<Integer, ServerSocket>();
        this.alldevs = new ArrayList<PcapIf>();
        this.errbuf = new StringBuilder();
        characterList = new LinkedList<>();
    }

    @Override
    public void run() {
        try {
            initalizePorts();
            initializeSniffer();
            initializeFilter();
            initializeHandler();
            initialiseFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /***************************************************************************
         * Fourth we enter the loop and tell it to capture 10 packets. The loop
         * method does a mapping of pcap.datalink() DLT value to JProtocol ID, which
         * is needed by JScanner. The scanner scans the packet buffer and decodes
         * the headers. The mapping is done automatically, although a variation on
         * the loop method exists that allows the programmer to sepecify exactly
         * which protocol ID to use as the data link type for this pcap interface.
         * -1 capture all
         **************************************************************************/
        pcap.loop(-1, jpacketHandler, "jNetPcap rocks!");
    }

    private void initalizePorts() throws IOException {
        portmap = new ArrayList<>();
        portmap.add(54);
        portmap.add(60);
        portmap.add(85);
        portmap.add(74);
        portmap.add(98);
        portmap.add(82);
        portmap.add(107);
        portmap.add(75);
        portmap.add(77);
        portmap.add(55);
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

    private void initializeFilter() {
        PcapBpfProgram program = new PcapBpfProgram();
        int optimize = 0;         // 0 = false
        int netmask = 0xFFFFFF00; // 255.255.255.0

        if (pcap.compile(program, FILTER, optimize, netmask) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }

        if (pcap.setFilter(program) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }
    }

    private void initializeHandler() {
        /***************************************************************************
         * Third we create a packet handler which will receive packets from the
         * libpcap loop.
         **************************************************************************/
        this.jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {

                Tcp tcp = new Tcp();

                if (packet.hasHeader(tcp)) {
                    //logMessage("Received packet on port: " + tcp.destination());
                    if (tcp.destination() == START_PORT) {
                        CHARACTER_COUNTER = 0;
                    } else if (tcp.destination() == END_PORT) {
                        characterList.add(CHARACTER_COUNTER);
                                logMessage("New character: " + CHARACTER_COUNTER);
                        try {
                            writeToFile((char) ((int)CHARACTER_COUNTER));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (portmap.contains(tcp.destination())) {
                        CHARACTER_COUNTER++;
                    } else if (tcp.destination() == FINISH_PORT) {
                        try {
                            finishTransmission();
                            main.java.ParserServer parserServer = new main.java.ParserServer(PATH);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    private void initialiseFile() throws FileNotFoundException {
        file = new File(PATH);
        fileOutputStream = new FileOutputStream(file);
    }

    private synchronized void writeToFile(char character) throws IOException {
        fileOutputStream.write(character);
    }

    private void finishTransmission() throws IOException {
        connectionsPool.forEach((number, connection) -> {
            try {
                connection.close();
                logMessage("Connection " + number + " closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileOutputStream.close();
        pcap.breakloop();
    }


    private void logMessage(String message) {
        System.out.println("[" + LocalDateTime.now().format(FORMAT) + "] " + message);
    }

    public static void main(String[] args) {
        new Server_TCP_v2().run();
    }
}
