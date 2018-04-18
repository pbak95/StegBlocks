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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client_TCP_v2 implements Runnable {

    private static final String FILE_BEFORE = "/home/osboxes/Studia/BEST/projekt/StegBlocks/test.txt";
    private static final String FILE_AFTER = "/home/osboxes/Studia/BEST/projekt/StegBlocks/test-encoded.txt";
    private static final int START_PORT = 122;    //could be changed later to spread ports across free space
    private static final int END_PORT = 150;    //could be changed later to spread ports across free space
    private static final int CONNECTIONS_NUMBER = 10; //number of ports used to transmit characters
    private static final int FINISH_PORT = 68;    //could be changed later to spread ports across free space
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static ArrayList<Integer> portmap;

    private String targetHost;
    private String currentHost;
    private boolean transmission;
    private boolean isScanOptionEnabled;
    private int scanPeriod;
    private int scanTimeUnit;
    private ByteBuffer filebuf;
    private List<PcapIf> alldevs; // Will be filled with NICs
    private StringBuilder errbuf; // For any error msgs
    private Pcap pcap; //sniffer
    private PcapPacketHandler<String> jpacketHandler; //packet handler
    private List<PcapPacket> tcpPacketPool;
    private List<String> hostsInNetwork;
    ScheduledExecutorService executor; //executor for scanning task



    public Client_TCP_v2(String targetHost, String currentHost, boolean transmission) {
        this.transmission = transmission;
        this.tcpPacketPool = new ArrayList<>();
        this.alldevs = new ArrayList<PcapIf>();
        this.errbuf = new StringBuilder();
        this.hostsInNetwork = Collections.synchronizedList(new ArrayList<>());
        this.targetHost = targetHost;
        this.currentHost = currentHost;
    }

    public Client_TCP_v2(String targetHost, String currentHost,  boolean transmission, boolean isScanOptionEnabled,
                         int scanPeriod, int scanTimeUnit) {
        this(targetHost, currentHost, transmission);
        this.isScanOptionEnabled = isScanOptionEnabled;
        this.scanPeriod = scanPeriod;
        this.scanTimeUnit = scanTimeUnit;
        if (isScanOptionEnabled)
            runHostDiscoveryTask();
        else
            mockHostDiscoveryTask();
    }

    @Override
    public void run() {
        try {
            initPorts();
            initializeFile();
            initializeSniffer();
            initializeHandler();
            if (transmission) {
                sendFile();
            } else {
                sendSomeTraffic();
            }
            executor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initPorts() {
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

    private void sendFile() throws UnknownHostException {
        logMessage("Start file transmission");
        filebuf.rewind();
        while (filebuf.hasRemaining()) {
            int sequence = filebuf.get();
            logMessage("Encoding: " + sequence);
            //pcap.loop(sequence + 2, jpacketHandler, "jNetPcap rocks!"); //character number + 2(start and end sequence)
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
        int temp = (int) (Math.random() * CONNECTIONS_NUMBER);
        return portmap.get(temp);
    }

    private void endTransmission() throws UnknownHostException {
        pcap.loop(1, jpacketHandler, "jNetPcap rocks!"); //get packet end
        sendPacket(tcpPacketPool.get(0), FINISH_PORT);
    }

    private void sendPacket(PcapPacket packetToOverwrite, int port) throws UnknownHostException {
        Ip4 ip = packetToOverwrite.getHeader(new Ip4());
        Tcp tcp = packetToOverwrite.getHeader(new Tcp());
        tcp.destination(port);
        ip.destination(Inet4Address.getByName(targetHost).getAddress());
        ip.source(getHostFromNetwork());
        //Recalculate packet checksum after overwrite
        ip.checksum(ip.calculateChecksum());
        tcp.checksum(tcp.calculateChecksum());
        //System.out.println(packetToOverwrite);
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
                    if (tcp.size() < 1800)
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

    private void runHostDiscoveryTask() {
        hostsInNetwork.add(this.currentHost);
        TimeUnit timeUnit;

        Runnable scanTask = () -> {
            String host="10.0.2" + "." +
                    Client_TCP_v2.randomNumberInRange(2,244);
            try {
                if (InetAddress.getByName(host).isReachable(1000)){
                    hostsInNetwork.add(host);
                    logMessage("New host discovered: " + host);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        if (scanTimeUnit == 1) {
            timeUnit = TimeUnit.MINUTES;
        } else {
            timeUnit = TimeUnit.SECONDS;
        }

        this.executor = Executors.newScheduledThreadPool(1);
        this.executor.scheduleAtFixedRate(scanTask, 0, scanPeriod, timeUnit);
    }

    private void mockHostDiscoveryTask() {
        String[] ipParts = this.currentHost.split("\\.");
        StringBuilder networkPrefix = new StringBuilder();
        networkPrefix.append(ipParts[0]);
        networkPrefix.append(".");
        networkPrefix.append(ipParts[1]);
        networkPrefix.append(".");
        networkPrefix.append(ipParts[2]);
        networkPrefix.append(".");
        String prefix = networkPrefix.toString();
        for (int i=1; i < 255; i++) {
            hostsInNetwork.add(prefix + i);
        }
    }

    private byte[] getHostFromNetwork() throws UnknownHostException {
        synchronized (hostsInNetwork) {
            return Inet4Address.getByName(hostsInNetwork.get(randomNumberInRange(0, hostsInNetwork.size() - 1))).getAddress();
        }
    }

    private void logMessage(String message) {
        System.out.println("[" + LocalDateTime.now().format(format) + "] " + message);
    }

    public static int randomNumberInRange(int min, int max) {
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    public static void handleMain(String[] args) {
        boolean transmission;
        boolean isScanOptionEnabled = false;
        int scanPeriod = 0;
        int scanTimeUnit = 0;

        if (args[2].equals("true") && args[2] != null) {
            transmission = true;
        } else {
            transmission = false;
        }

        if (args.length > 3) {

         if (args[3].equals("true")) {
            isScanOptionEnabled = true;
            scanPeriod = Integer.parseInt(args[4]);
            scanTimeUnit = Integer.parseInt(args[5]);
         } else {
             isScanOptionEnabled = false;
         }

        }

        if (args.length > 3) {
            new Client_TCP_v2(args[0], args[1], transmission, isScanOptionEnabled, scanPeriod, scanTimeUnit).run();
        } else {
            new Client_TCP_v2(args[0], args[1], transmission).run();
        }

    }

    public static void main(String[] args) {

        Client_TCP_v2.handleMain(args);

//        if(args[0].equals("true"))
//            new Client_TCP_v2(true).run();
//
//        else
//            new Client_TCP_v2(false).run();
    }

}
