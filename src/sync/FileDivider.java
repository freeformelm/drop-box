package sync;

import java.io.*;
import java.util.Arrays;

public class FileDivider {

    public static void main(String[] args) {
        String filename = "/Users/djoshi/Downloads/drop-box/client2/abc.txt"; // replace with your filename
        int blockSize = 30; // block size in bytes
        int lineToChange = 19; // index of the line to change (starting from 0)

        try {
            // read the file into a byte array
            File file = new File(filename);
            byte[] fileData = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileData);
            fileInputStream.close();

            // divide the file into blocks
            int numBlocks = (int) Math.ceil((double) fileData.length / blockSize);
            System.out.println("Number of blocks: "+ numBlocks);
            byte[][] blocks = new byte[numBlocks][];
            for (int i = 0; i < numBlocks; i++) {
                int offset = i * blockSize;
                int size = Math.min(blockSize, fileData.length - offset);
                blocks[i] = new byte[size];
                System.arraycopy(fileData, offset, blocks[i], 0, size);
            }

            // change one line in the file
            String fileContent = new String(fileData);
            String[] lines = fileContent.split("\\r?\\n"); // split by newline characters
            lines[lineToChange] = "This is line 21.";
            fileContent = String.join(System.lineSeparator(), lines);
            byte[] newData = fileContent.getBytes();
            System.arraycopy(newData, 0, fileData, 0, newData.length);

            // detect which block has changed
            for (int i = 0; i < numBlocks; i++) {
                boolean blockChanged = !Arrays.equals(
                        Arrays.copyOfRange(blocks[i], 0, Math.min(blocks[i].length, fileData.length - i * blockSize)),
                        Arrays.copyOfRange(fileData, i * blockSize, Math.min(fileData.length, (i + 1) * blockSize))
                );

                if (blockChanged) {
                    System.out.println("Block " + i + " has changed");
                    System.out.println(newData[i]);
                    System.out.println(newData[i]);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
