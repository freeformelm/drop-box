package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;

public class Helper {

    /**
     * save the first 4 bytes of bytes1 to the first 4 bytes of bytes2
     */
    public static void save4Bytes(byte[] bytes1, byte[] bytes2) {
        for (int i = 0; i < 4; i++) {
            bytes2[i] = bytes1[i];
        }
    }

    /**
     * get the first 4 bytes from bytes2
     */
    public static byte[] get4Bytes(byte[] bytes2) {
        byte[] get = new byte[4];
        for (int i = 0; i < 4; i++) {
            get[i] = bytes2[i];
        }
        return get;
    }

    /**
     * turn an integer a to byte array of size 4.
     */
    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * turn a byte array of size 4 to an integer
     */
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    /**
     * takes file path as input and returns true if path leads to valid directory
     */
    public static boolean checkPath(String path) {
        boolean isValid = false;
        try {
            File test = new File(path); // throws exception if invalid path
            if (test.isFile()) // false if it's file, not folder
            {
                isValid = false;
            } else if (test.isDirectory()) // return true only if path leads to valid directory
            {
                isValid = true;
            }
        } catch (Exception e) {
            isValid = false;
            e.printStackTrace();
        }
        return isValid;
    }

    public static List<String> getAllFileNames(String directoryPath) {

        // Create a File object for the directory
        File directory = new File(directoryPath);

        return Arrays.stream(directory.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());

    }

    public static List<String> calculateBlockHashesOfTheFile(String filePath, Integer blockSize)
            throws IOException, NoSuchAlgorithmException {
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        long fileSize = file.length();

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Considering buffer size and file block size are equal
        byte[] buffer = new byte[blockSize - 4];

        int bytesRead;
        List<String> blockHashes = new ArrayList<>();
        while ((bytesRead = fileInputStream.read(buffer)) > 0) {
            md.update(buffer, 0, bytesRead);
            byte[] digest = md.digest();
            String blockHash = DatatypeConverter.printHexBinary(digest);
            blockHashes.add(blockHash);
        }
        fileInputStream.close();
        return blockHashes;
    }

    public static List<byte[]> getBlocksToSend(String filePath, List<Integer> blockNumbers, Integer blockSize)
            throws FileNotFoundException {
        List<byte[]> byteList = new ArrayList<>();
        List<byte[]> changedBytes = new ArrayList<>();
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[blockSize - 4];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] temp = new byte[bytesRead];
                System.arraycopy(buffer, 0, temp, 0, bytesRead);
                byteList.add(temp);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Integer block : blockNumbers){
            changedBytes.add(byteList.get(block));
        }
        return changedBytes;
    }

    public static List<byte[]> getAllFileBlocks(String filePath, Integer blockSize)
            throws FileNotFoundException {
        List<byte[]> byteList = new ArrayList<>();
        List<byte[]> changedBytes = new ArrayList<>();
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[blockSize - 4];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] temp = new byte[bytesRead];
                System.arraycopy(buffer, 0, temp, 0, bytesRead);
                byteList.add(temp);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
       return byteList;
    }
    public byte[] readFileInByteArray(String filepath) throws IOException {
        File file = new File(filepath);
        byte[] fileData = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(fileData);
        fileInputStream.close();
        return fileData;
    }


}
