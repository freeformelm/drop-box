public class Network {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 1234;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void connect() throws IOException {
        socket = new Socket(HOSTNAME, PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public static void disconnect() throws IOException {
        out.close();
        in.close();
        socket.close();
    }

    public static void sendRequest(DeltaSyncRequest request) throws IOException {
        out.writeObject(request);
        out.flush();
    }

    public static DeltaSyncResponse receiveResponse() throws IOException, ClassNotFoundException {
        return (DeltaSyncResponse) in.readObject();
    }
}
