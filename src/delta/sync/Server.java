public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

            // Receive a delta sync request from the client
            DeltaSyncRequest request = (DeltaSyncRequest) in.readObject();
            int clientVersion = request.getClientVersion();

            // Compute the changes since the client's version
            List<FileChangeEvent> changes = computeChangesSinceVersion(clientVersion);

            // Send the delta sync response to the client
            DeltaSyncResponse response = new DeltaSyncResponse(changes);
            out.writeObject(response);
            out.flush();

            in.close();
            out.close();
            clientSocket.close();
        }
    }

    private static List<FileChangeEvent> computeChangesSinceVersion(int clientVersion) {
        List<FileChangeEvent> changes = new ArrayList<>();

        try {
            // Retrieve the list of files and directories in the root directory of the
            // server
            Path serverRoot = Paths.get("server");
            List<Path> serverEntries = Files.list(serverRoot).collect(Collectors.toList());

            // Iterate over each file or directory in the server root directory
            for (Path serverEntry : serverEntries) {
                String relativePath = serverRoot.relativize(serverEntry).toString();

                // Compute the checksum of the file or directory
                byte[] checksum = null;
                if (Files.isDirectory(serverEntry)) {
                    checksum = computeDirectoryChecksum(serverEntry);
                } else {
                    checksum = computeFileChecksum(serverEntry);
                }

                // Check if the file or directory has been modified since the client's version
                if (clientVersion < getFileVersion(relativePath) || clientVersion < getDirectoryVersion(relativePath)) {
                    byte[] clientChecksum = getClientChecksum(relativePath);

                    if (!Arrays.equals(checksum, clientChecksum)) {
                        // The file or directory has been modified
                        if (Files.isDirectory(serverEntry)) {
                            changes.add(new DirectoryChangeEvent(relativePath, DirectoryChangeType.MODIFIED));
                        } else {
                            changes.add(new FileChangeEvent(relativePath, FileChangeType.MODIFIED));
                        }
                    }
                } else {
                    // The file or directory has not changed
                }
            }

            // Check for files or directories that have been deleted
            for (String relativePath : getClientEntries()) {
                Path serverEntry = serverRoot.resolve(relativePath);

                if (!Files.exists(serverEntry)) {
                    if (Files.isDirectory(serverEntry)) {
                        changes.add(new DirectoryChangeEvent(relativePath, DirectoryChangeType.DELETED));
                    } else {
                        changes.add(new FileChangeEvent(relativePath, FileChangeType.DELETED));
                    }
                }
            }

            // Check for files or directories that have been added
            for (Path serverEntry : serverEntries) {
                String relativePath = serverRoot.relativize(serverEntry).toString();

                if (!getClientEntries().contains(relativePath)) {
                    if (Files.isDirectory(serverEntry)) {
                        changes.add(new DirectoryChangeEvent(relativePath, DirectoryChangeType.ADDED));
                    } else {
                        changes.add(new FileChangeEvent(relativePath, FileChangeType.ADDED));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return changes;
    }

    private static byte[] computeFileChecksum(Path file) throws IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }
        return md.digest();
    }

    private static byte[] computeDirectoryChecksum(Path directory) throws IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        // Iterate over the files and directories in the directory
        List<Path> entries = Files.list(directory).collect(Collectors.toList());
        for (Path entry : entries) {
            // Compute the checksum of the file or directory
            byte[] checksum = null;
            if (Files.isDirectory(entry)) {
                checksum = computeDirectoryChecksum(entry);
            } else {
                checksum = computeFileChecksum(entry);
            }
            md.update(checksum);
        }

        return md.digest();
    }

}
