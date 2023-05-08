package sync;

import common.Constants;
import common.Helper;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class ReverseSync implements Runnable {

    static String folder;
    static Integer udpPort;

    public ReverseSync(String folder, Integer udpPort){
        this.folder = folder;
        this.udpPort = udpPort;
    }

    @Override
    public void run() {
        InetAddress serverIp = null;
        try {
            serverIp = InetAddress.getByName("localhost");
            Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

            String action = "SEND REVERSE SYNC REQUEST";
            int serverPort = getReverseSyncPortFromServer(tcpSocket, action, Helper.getAllFileNames(
                    folder), udpPort);
            if (serverPort == 0) {
                return;
            }
            receiveHandle(serverPort);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getReverseSyncPortFromServer(Socket tcpSocket, String action, List<String> files, int udpPort) {
        int serverPort = 0;
        try {
            Scanner inputSocket = new Scanner(tcpSocket.getInputStream());
            PrintWriter outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);

            String fileNames = String.join("&", files);

            // send the HTTP packet
            String request = action + " # " + fileNames + " # " + udpPort;
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

    public static void receiveHandle(int senderPort) {
        try {

            PacketBoundedBufferMonitor bm = new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp = InetAddress.getByName("localhost");
            ;// get the IP of the sender
            InetAddress receiverIp = InetAddress.getByName("localhost");
            receiveFile(bm, receiverIp, udpPort, senderIp, senderPort);// receive the file

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void receiveFile(PacketBoundedBufferMonitor bm, InetAddress receiverIp, int receiverPort,
            InetAddress senderIp, int senderPort) {

        PacketReceiver packetReceiver = new PacketReceiver(bm, receiverIp, receiverPort, senderIp, senderPort);
        packetReceiver.start();

        FileWriter fileWriter = new FileWriter(bm, folder);
        fileWriter.start();
        try {
            packetReceiver.join();
            fileWriter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
