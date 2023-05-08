package sync;

import common.Constants;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client1 {

    public Client1() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Thread sync = new Thread(
                new ReverseSync(Constants.CLIENT_1_FILE_ROOT, Constants.REVERSE_SYNC_UDP_PORT_CLIENT_1));
        executor.scheduleAtFixedRate(sync, 0, 30, TimeUnit.SECONDS);

        String directoryPath = Constants.CLIENT_1_FILE_ROOT;

        WatchService watchService = FileSystems.getDefault().newWatchService();

        Path path = Paths.get(directoryPath);

        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        WatchKey key;
        int counter = 0;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                System.out.println(
                        "Event kind:" + event.kind()
                                + ". File affected: " + event.context() + ".");
                switch (event.kind().toString()) {
                    case "ENTRY_MODIFY":
                    case "ENTRY_CREATE":
                        syncFile(event.context().toString(), Constants.CLIENT_UDP_PORT_CLIENT1 + counter);
                        break;
                    case "ENTRY_DELETE":
                        deleteFile(event.context().toString(), Constants.CLIENT_UDP_PORT_CLIENT1 + counter);
                        break;
                }
                counter++;
            }
            key.reset();
        }
    }

    private static void deleteFile(String fileName, int udpPort) throws IOException {
        InetAddress serverIp = InetAddress.getByName("localhost");
        Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

        // get the port number from the server that will receive data through UDP
        // datagrams
        String action = "DELETE REQUEST";
        int serverPort = getPortFromServer(tcpSocket, action, fileName, udpPort);
    }

    private static void syncFile(String fileName, int udpPort) {
        try {
            File file = new File(Constants.CLIENT_1_FILE_ROOT + fileName);
            if (!file.exists()) {
                return;
            }

            InetAddress serverIp = InetAddress.getByName("localhost");
            Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

            // get the port number from the server that will receive data through UDP
            // datagrams
            String action = "SEND REQUEST";
            int serverPort = getPortFromServer(tcpSocket, action, fileName, udpPort);
            if (serverPort == 0) {
                return;
            }

            // start sending the file
            PacketBoundedBufferMonitor bufferMonitor = new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp = InetAddress.getByName("localhost");

            PacketSender packetSender = new PacketSender(bufferMonitor, senderIp, udpPort, serverIp,
                    serverPort);
            packetSender.start();

            FileReader fileReader = new FileReader(bufferMonitor, fileName, Constants.CLIENT_1_FILE_ROOT);
            fileReader.start();

            try {
                packetSender.join();
                fileReader.join();
            } catch (InterruptedException e) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get the port number, through which the server will receive data
     *
     * @param udpPort, the port number that the client will send data
     */
    public static int getPortFromServer(Socket tcpSocket, String action, String fileName, int udpPort) {
        int serverPort = 0;
        try {
            Scanner inputSocket = new Scanner(tcpSocket.getInputStream());
            PrintWriter outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);

            // send the HTTP packet
            String request = action + " # " + fileName + " # " + udpPort;
            outputSocket.println(request + Constants.CRLF + "STOP");
            System.out.println(Constants.CRLF + ">> Request:" + request);

            // receive the response
            String line = inputSocket.nextLine();

            // get the port number of the server that will receive data for the file
            while (!line.equals("STOP")) {
                if (line.isEmpty()) {
                    line = inputSocket.nextLine();
                    continue;
                }
                if (line.startsWith(action)) {
                    // get the new port that is assigned by the server to receive data
                    System.out.println(">> Response:" + line + Constants.CRLF);
                    String[] items = line.split(":");
                    serverPort = Integer.parseInt(items[items.length - 1]);
                    break;
                }
                line = inputSocket.nextLine();
            }
            inputSocket.close();
            outputSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverPort;
    }

}
