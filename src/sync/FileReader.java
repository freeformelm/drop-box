package sync;

import common.Constants;
import java.io.File;
import java.io.FileInputStream;

public class FileReader extends Thread {
	private String threadName="sync.FileReader";
	private PacketBoundedBufferMonitor bufferMonitor;	
	private String fileName;
	private String directory;
	
	
	public FileReader() {}
	public FileReader(PacketBoundedBufferMonitor bm, String fileName, String directory) {
		this.bufferMonitor=bm;		
		this.fileName=fileName;
		this.directory = directory;
	}
	
	public void run() {
		try {
			File file=new File(directory+fileName);
			FileInputStream in = new FileInputStream (file);				
			boolean flagFileHead=true;
			int packetIndex=0;
			int readSize=0;
	
			System.out.println(">> Begin to read a file"+Constants.CRLF);
			while(true) {
				if (flagFileHead) {
					flagFileHead=false;	
					// prepare the head packet containing file name
					String fileHead="fileName:"+fileName;
					Packet pkt=new Packet(packetIndex,fileHead.getBytes(),fileHead.getBytes().length);
					System.out.println(Constants.CRLF+">> Prepare data for the head packet with index: "+pkt.getIndex());
								
					// deposit the packet
					this.bufferMonitor.deposit(pkt);					
				}
				else {
					// prepare for the rest packets that contains content of the file
					packetIndex+=1;	
					byte[] buf=new byte[Constants.MAX_DATAGRAM_SIZE-4]; // the first 4 bytes are reserved for packet index.
					readSize=in.read(buf,0,buf.length);
					
					if (readSize==-1) { // end of reading a file
						Packet pkt=new Packet(-1,"End of reading a file".getBytes(),0);
						this.bufferMonitor.deposit(pkt);						
						System.out.println(">> Finish reading the file: "+fileName+ Constants.CRLF);
						break;
					
					}else {
						Packet pkt=new Packet(packetIndex,buf,readSize);						
						System.out.println(">> Read from a file for the packet with index "+pkt.getIndex());
				
						this.bufferMonitor.deposit(pkt);
					}
				} 
			
			}// end of file sending	
			
		}catch(Exception e) {e.printStackTrace();}
	}
}
