package delta.sync;

import common.Constants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import sync.Packet;
import sync.PacketBoundedBufferMonitor;

public class DeltaFileWriter extends Thread {

    private String threadName = "sync.DeltaFileWriter";
    private PacketBoundedBufferMonitor bufferMonitor;
    FileChannel channel = null;
    String filePath = null;

    public DeltaFileWriter() {
    }

    public DeltaFileWriter(PacketBoundedBufferMonitor bm, String filePath) {
        this.bufferMonitor = bm;
        this.filePath = filePath;
    }

    public void run() {
        try {
            String fileName = "";
            ByteBuffer buffer = ByteBuffer.allocate(Constants.MAX_DATAGRAM_SIZE);
            List<Integer> packetIndices = new ArrayList<>();

            System.out.println(">> Begin to write packets to a file" + Constants.CRLF);
            boolean newFile = false;
            while (true) {

                Packet pkt = this.bufferMonitor.withdraw();

                if (pkt.getIndex() == -1) {
                    System.out.println(">> Finish saving the file:" + fileName);
                    System.out.println(">> sync.Packet indices: " + packetIndices);
                    break;
                }

                if (pkt.getIndex() == 0) {
                    // read the head packet
                    String msg = pkt.getContentInString();
                    fileName = msg.split(":")[1]; // head packet content: "fileName: file name"
                    File file = new File(filePath + fileName);
                    newFile = !file.exists();
                    if (newFile) {
                        file.createNewFile();
                        channel = new FileOutputStream(file, false).getChannel();
                        System.out.println(
                                Constants.CRLF + ">> Prepare to write the file " + fileName + Constants.CRLF);
                    }

                } else {
                    // write packets tp a file
                    if (newFile) {
                        System.out.println(">> Write to a file the packet with index " + pkt.getIndex());
                        buffer = ByteBuffer.wrap(pkt.getContent(), 0, pkt.getContentSize());
                        channel.write(buffer);
                        packetIndices.add(pkt.getIndex());
                    } else {
                        System.out.println(">> Write to a file the packet with index " + pkt.getIndex());
                        handleDeltaReceive(filePath + fileName, pkt);
                        packetIndices.add(pkt.getIndex());
                    }
                }

            }//end of writing a file

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }// end of run()

    private void handleDeltaReceive(String s, Packet pkt) throws IOException {

        RandomAccessFile file = new RandomAccessFile(s, "rw");
        int index = pkt.getIndex();
        int seekPosition = (index-1) * Constants.MAX_DATAGRAM_SIZE;
        // seek to the position where you want to modify bytes
        file.seek(seekPosition);

        // write new bytes at the position
        file.write(pkt.getContent());

        // close the file
        file.close();
    }

}
