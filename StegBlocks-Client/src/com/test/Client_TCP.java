import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Patryk on 21.03.2018.
 */
public class Client_TCP implements Runnable {

    private static String TMP_PATH = "D:\\studia\\BEST\\projekt\\StegBlocks\\test-new.txt";
    private static int SERVER_PORT = 34562;
    private static String HOST = "localhost";
    private static final int CONNECTIONS_NUMBER = 4;
    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private File file;
    private FileOutputStream fileOutputStream;
    private Map<Integer, Socket> connectionsPool;
    private Map<Socket, BufferedReader> inputStreams;

    public Client_TCP() {
        connectionsPool = new LinkedHashMap<>();
        inputStreams = new HashMap<>();
        run();
    }

    public void run() {
        try {
            initialiseFile();
            initialiseConnectionsPool();
            connectionsPool.forEach((connectionNumber, connection) ->
                    new Thread(() -> {
                readDataFromServer(connectionNumber, inputStreams.get(connection));
                    }).start()
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readDataFromServer(int connectionNumber, BufferedReader bufferedReader) {
        logMessage("Hello from connection " + connectionNumber);
        while (true) {
            try {
                String message = bufferedReader.readLine();
                if (message != null) {
                    if (message.equals("END")) {
                        transmissionFinished();
                    } else {
                        writeToFile(connectionNumber + message + "%n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void writeToFile(String text) throws IOException {
        fileOutputStream.write(text.getBytes());
        logMessage("Message wrote to file: " + text);
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

    private void transmissionFinished() {
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
