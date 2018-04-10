import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Patryk on 21.03.2018.
 */
public class Client_TCP implements Runnable {

    private static String TMP_PATH = "antygona-new.txt";
    private static int SERVER_PORT = 34562;
    private static String HOST = "localhost";
    private static final int CONNECTIONS_NUMBER = 4;
    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int START_MSG_CONN = 0;
    private static final int END_MSG_CONN = 3;

    private static volatile int CHARACTER_COUNTER = 0;

    private File file;
    private FileOutputStream fileOutputStream;
    private Map<Integer, Socket> connectionsPool;
    private Map<Socket, BufferedReader> inputStreams;

    public List<Integer> characterList;

    public Client_TCP() {
        connectionsPool = new LinkedHashMap<>();
        inputStreams = new HashMap<>();
        characterList = new LinkedList<>();
        run();
    }

    public void run() {
        try {
            initialiseFile();
            initialiseConnectionsPool();
            connectionsPool.forEach((connectionNumber, connection) ->
                    new Thread(() -> {
                        try {
                            readDataFromServer(connectionNumber, inputStreams.get(connection));
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }).start()
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readDataFromServer(int connectionNumber, BufferedReader bufferedReader) throws IOException {
        logMessage("Hello from connection " + connectionNumber);
        while (true) {
                String message = bufferedReader.readLine();
                if (message != null) {
                    if (message.trim().equals("END")) {
                        System.out.println("End");
                        Parser parser = new Parser(TMP_PATH);
                        break;
                    } else {
                        switch (connectionNumber) {
                            case START_MSG_CONN:
                                CHARACTER_COUNTER = 0;
                                break;
                            case END_MSG_CONN:
                                characterList.add(CHARACTER_COUNTER);
                                try {
                                    writeToFile((char) ((int)CHARACTER_COUNTER));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                CHARACTER_COUNTER++;
                                break;
                        }
                    }
                }
        }
        finishTransmission();
        fileOutputStream.close();
    }

    private synchronized void writeToFile(char character) throws IOException {
        fileOutputStream.write(character);
    }

    private void initialiseFile() throws FileNotFoundException {
        file = new File(TMP_PATH);
        fileOutputStream = new FileOutputStream(file);
    }

    private void initialiseConnectionsPool() {
        for (int i = 0; i < CONNECTIONS_NUMBER; i++) {
            try {
                Socket clientSocket = new Socket(HOST, SERVER_PORT);
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );
                connectionsPool.put(i, clientSocket);
                inputStreams.put(clientSocket, bufferedReader);
                logMessage("Connection " + i + " created");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void finishTransmission() throws IOException {
        inputStreams.forEach(((socket, bufferedReader) -> {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        connectionsPool.forEach((number, connection) -> {
            try {
                connection.close();
                logMessage("Connection " + number + " closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private void logMessage(String message) {
        System.out.println("[" + LocalDateTime.now().format(format) + "] " + message);
    }

    public static void main(String args[]) {
        new Client_TCP();
    }
}
