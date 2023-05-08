package sync;

import common.Constants;
import common.Helper;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Server {

    public Server() {
    }

    public static void main(String[] args) {
        int reverseSyncCounter = 0;
        try {

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(Constants.SERVER_TCP_PORT);
            } catch (IOException ioEx) {
                System.out.println("\n>> Unable to set up port!");
                System.exit(1);
            }

            System.out.println("\r\n>> Ready to accept requests");
            // handle multiple client connections
            do {
                try {
                    // Wait for clients...
                    Socket client = serverSocket.accept();
                    System.out.println("\n>> New request is accepted." + Constants.CRLF);

                    Scanner inputSocket = new Scanner(client.getInputStream());
                    PrintWriter outputSocket = new PrintWriter(client.getOutputStream(), true);

                    // get action type from the received data
                    String line = inputSocket.nextLine();
                    String actionType = "";
                    int clientUDPPort = 0;
                    String filename = null;
                    String syncFilename = null;
                    while (!line.equals("STOP")) {
                        if (line.isEmpty()) {
                            line = inputSocket.nextLine();
                            continue;
                        }
                        if (line.startsWith("SEND REQUEST")) {
                            System.out.println(">> Request: " + line + Constants.CRLF);
                            actionType = "SEND REQUEST";
                            clientUDPPort = Integer.parseInt(line.split("#")[2].strip());
                            break;
                        }
                        if (line.startsWith("DELETE REQUEST")) {
                            System.out.println(">> Request: " + line + Constants.CRLF);
                            actionType = "DELETE REQUEST";
                            clientUDPPort = Integer.parseInt(line.split("#")[2].strip());
                            filename = line.split("#")[1].strip();
                            break;
                        }
                        if (line.startsWith("SEND REVERSE SYNC REQUEST")) {
                            System.out.println(">> Request: " + line + Constants.CRLF);
                            actionType = "SEND REVERSE SYNC REQUEST";
                            clientUDPPort = Integer.parseInt(line.split("#")[2].strip());
                            syncFilename = getFileToSyncBack(line.split("#")[1].strip());
                            break;
                        }
                        line = inputSocket.nextLine();
                    }

                    if (actionType.equals("SEND REQUEST")) {
                        receiveHandle(client, outputSocket, clientUDPPort);
                    }

                    if (actionType.equals("DELETE REQUEST")) {
                        receiveHandleDelete(client, outputSocket, clientUDPPort, filename);
                    }
                    if (actionType.equals("SEND REVERSE SYNC REQUEST")) {
                        reverseSyncFile(client, outputSocket, syncFilename, clientUDPPort, reverseSyncCounter);
                        reverseSyncCounter++;
                    }

                } catch (IOException io) {
                    System.out.println(">> Fail to listen to requests!");
                    System.exit(1);
                }

            } while (true);// end of while loop

        } catch (Exception e) {
            e.printStackTrace();
        }

    }// end of main

    public static void reverseSyncFile(Socket socket, PrintWriter outputSocket, String filename, int toPort,
            int reverseSyncCounter)
            throws UnknownHostException {
        if (filename != null) {
            int port = Constants.SERVER_UDP_PORT_SYNC + reverseSyncCounter;
            String response =
                    "SEND REVERSE SYNC REQUEST OK: receive data with the port:" + port;
            System.out.println(">> Response: " + response + Constants.CRLF);

            // send the response
            outputSocket.println(response + Constants.CRLF + "STOP");
            outputSocket.close();

            PacketBoundedBufferMonitor bufferMonitor = new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress fromIp = InetAddress.getByName("localhost");
            InetAddress toIp = socket.getInetAddress();

            PacketSender packetSender = new PacketSender(bufferMonitor, fromIp,
                    port, toIp,
                    toPort);
            packetSender.start();

            FileReader fileReader = new FileReader(bufferMonitor, filename, Constants.SERVER_FILE_ROOT);
            fileReader.start();

            try {
                packetSender.join();
                fileReader.join();
            } catch (InterruptedException e) {
            }
        } else {
            String response =
                    "SEND REVERSE SYNC REQUEST NOTHING TO SYNC: receive data with the port:"
                            + 0;
            System.out.println(">> Response: " + response + Constants.CRLF);

            // send the response
            outputSocket.println(response + Constants.CRLF + "STOP");
            outputSocket.close();
        }

    }

    private static String getFileToSyncBack(String files) {
        List<String> filesInClient =
                files.isEmpty() ? new ArrayList<>() : Arrays.stream(files.split("&")).collect(Collectors.toList());
        List<String> filesInServer = Helper.getAllFileNames(Constants.SERVER_FILE_ROOT);

       /* for (String file : filesInServer){
            if(!filesInClient.contains(file)){
                differences.add(file);
            }
        } */

        List<String> differences = filesInServer.stream()
                .filter(element -> !filesInClient.contains(element))
                .collect(Collectors.toList());

        return differences.size() == 0 ? null : differences.get(0);
    }

    public static void receiveHandleDelete(Socket socket, PrintWriter outputSocket, int senderPort,
            String fileToDelete) {
        try {
            // create the response with the port number which will receive data from clients through UDP
            String response = "DELETE REQUEST OK: receive data with the port:" + Constants.SERVER_UDP_PORT;
            System.out.println(">> Response: " + response + Constants.CRLF);

            File file = new File(Constants.SERVER_FILE_ROOT + fileToDelete);
            if (file.delete()) {
                System.out.println("  File deleted successfully");
            } else {
                System.out.println("  Failed to delete the file");
            }

            // send the response
            outputSocket.println(response + Constants.CRLF + "STOP");
            outputSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void receiveHandle(Socket socket, PrintWriter outputSocket, int senderPort) {
        try {
            // create the response with the port number which will receive data from clients through UDP
            String response = "SEND REQUEST OK: receive data with the port:" + Constants.SERVER_UDP_PORT;
            System.out.println(">> Response: " + response + Constants.CRLF);

            // send the response
            outputSocket.println(response + Constants.CRLF + "STOP");
            outputSocket.close();

            PacketBoundedBufferMonitor bm = new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp = socket.getInetAddress();// get the IP of the sender
            InetAddress receiverIp = InetAddress.getByName("localhost");

            receiveFile(bm, receiverIp, Constants.SERVER_UDP_PORT, senderIp, senderPort);// receive the file

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void receiveFile(PacketBoundedBufferMonitor bm, InetAddress receiverIp, int receiverPort,
            InetAddress senderIp, int senderPort) {

        PacketReceiver packetReceiver = new PacketReceiver(bm, receiverIp, receiverPort, senderIp, senderPort);
        packetReceiver.start();

        FileWriter fileWriter = new FileWriter(bm, Constants.SERVER_FILE_ROOT);
        fileWriter.start();
        try {
            packetReceiver.join();
            fileWriter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}

