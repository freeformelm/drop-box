public class DeltaSyncRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int clientVersion;

    public DeltaSyncRequest(int clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getClientVersion() {
        return clientVersion;
    }
}
