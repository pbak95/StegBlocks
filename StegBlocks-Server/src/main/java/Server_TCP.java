import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Patryk on 21.03.2018.
 */
public class Server_TCP implements Runnable {

    private static String FILE_BEFORE = "antygona.txt";
    private static String FILE_AFTER = "antygona-encoded.txt";
    private static int CONNECTION_COUNTER = 0;
    private static final int START_MSG_CONN = 0;
    private static final int END_MSG_CONN = 3;
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int CONNECTIONS_NUMBER = 4;

    private int SERVER_PORT = 34562;
    private ServerSocket serverSocket;
    private ByteBuffer filebuf;
    private Map<Integer, Socket> connections;
    private Map<Socket, PrintStream> streams;
    private Timer timer;

    private boolean encoding = false;

    public Server_TCP(boolean flag) {
        try {
            timer = new Timer();
            encoding = flag;
            serverSocket = new ServerSocket(SERVER_PORT);
            connections = new HashMap();
            streams = new HashMap<>();
            Parser parser = new Parser(FILE_BEFORE);
            readFile(FILE_AFTER); //in future pass param from console through constructor to this method
            run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        logMessage("Server started");
        while (true) {
            try {
                Socket client = serverSocket.accept();
                logMessage("New client connection: " + CONNECTION_COUNTER);
                connections.put(CONNECTION_COUNTER++, client);
                filebuf.rewind();
                if (connections.size() == CONNECTIONS_NUMBER) {
                    logMessage("Start" + encoding);
                    if(encoding) {
                        while (filebuf.hasRemaining()) {
                            int sequence = filebuf.get();
                            logMessage("Encoding: " + sequence);
                            sendDate(LocalDateTime.now().format(format), sequence);
                        }
                        sendDate("END", START_MSG_CONN);
                        finishTransmission();
                        CONNECTION_COUNTER = 0;
                    }
                    else {
                        //sending every 10ms current date
                        timer.schedule( new TimerTask()
                        {
                            public void run() {
                                try {
                                    sendDate(LocalDateTime.now().format(format), 0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 0, 10);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDate(String date, int sequence) throws IOException {
        //send first token
        sendStream(connections.get(START_MSG_CONN),date,START_MSG_CONN);
        for(int i=0; i < sequence; i++) {
            int streamno = (int) (1 + (Math.random() * (CONNECTIONS_NUMBER - 2))); //-2 because start and end token
            sendStream(connections.get(streamno), date, streamno);
        }
        //send end token
        sendStream(connections.get(END_MSG_CONN),date,END_MSG_CONN);
    }

    private void sendStream(Socket socket, String date, int connectionNumber) throws IOException {
        PrintStream stream = null;
        if (!streams.containsKey(socket)) {
            stream = new PrintStream(socket.getOutputStream());
            streams.put(socket, stream);
        } else {
           stream = streams.get(socket);
        }
        try {
            if(encoding) {
                Thread.sleep((int) (1 + (Math.random() * 10))); //ugly but works, small delay to overpass delays in channel
            }
            stream.println(date);
            logMessage("Sent: " + "[" + connectionNumber + "] " + date);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void finishTransmission() {
        streams.forEach((socket, stream) -> stream.close());
        connections.forEach((number, socket) -> {
            try {
                socket.close();
                logMessage("Connection " + number + " closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void readFile(String path) throws IOException {
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
            new Server_TCP(true);
        }
        else {
            new Server_TCP(false);
        }
    }
}
