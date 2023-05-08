package delta.sync;

import common.Constants;
import common.Helper;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import sync.Packet;
import sync.PacketBoundedBufferMonitor;

public class DeltaFileReader extends Thread {

    private String threadName = "delta.sync.DeltaFileReader";
    private PacketBoundedBufferMonitor bufferMonitor;
    private String fileName;
    private String directory;

    private List<Integer> blockNumbers; // If not null, reader is in delta sync mode

    public DeltaFileReader() {
    }

    public DeltaFileReader(PacketBoundedBufferMonitor bm, String fileName, String directory,
            List<Integer> blockNumbers) {
        this.bufferMonitor = bm;
        this.fileName = fileName;
        this.directory = directory;
        this.blockNumbers = blockNumbers;
    }

    public void run() {
        try {
            File file = new File(directory + fileName);
            FileInputStream in = new FileInputStream(file);
            int packetIndex = 0;

            List<byte[]> blocksToSend = Helper.getBlocksToSend(directory + fileName, blockNumbers,
                    Constants.MAX_DATAGRAM_SIZE);

            System.out.println(">> Begin to read changed blocks of a file" + Constants.CRLF);

            String fileHead = "fileName:" + fileName;
            Packet pkt = new Packet(packetIndex, fileHead.getBytes(), fileHead.getBytes().length);
            System.out.println(
                    Constants.CRLF + ">> Prepare data for the head packet with index: " + pkt.getIndex());

            // deposit the packet
            this.bufferMonitor.deposit(pkt);
            for (int i = 0; i < blocksToSend.size(); i++) {
                Packet pkt1 = new Packet(blockNumbers.get(i) + 1, blocksToSend.get(i), blocksToSend.get(i).length);
                System.out.println(">> Read from a file for the packet with index " + pkt1.getIndex());
                this.bufferMonitor.deposit(pkt1);
            }
            Packet endpkt = new Packet(-1, "End of reading a file".getBytes(), 0);
            this.bufferMonitor.deposit(endpkt);
            System.out.println(">> Finish reading changed blocks of the file: " + fileName + Constants.CRLF);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
