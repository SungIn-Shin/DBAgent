package hpr.que.filedb;

import java.io.IOException;
import java.nio.channels.FileLock;

public class DiskFile extends PhysicalFile {

	public void get( long position, byte[] dst, int length ) throws IOException {
	
		randomAccessFile_.seek( position );
		randomAccessFile_.read( dst, 0, length );
	}
	
	public void put( long position, byte[] src, int offset, int length ) throws IOException {
		randomAccessFile_.seek( position );
		randomAccessFile_.write( src, offset, length );
	}
	
	public int getInt( long position ) throws IOException {

		randomAccessFile_.seek( position );
		return randomAccessFile_.readInt();
	}

	public void putInt( long position, int value ) throws IOException {

		randomAccessFile_.seek( position );
		randomAccessFile_.writeInt( value );
	}
	
	// public static void main(String[] args) {
	
	// 	DiskFile mapFile = new DiskFile();
		
	// 	try {
	// 		mapFile.createOpen("DiskFile.txt");
			
	// 		FileLock lock = null;
	// 		try {
	// 			lock = mapFile.getLock();
				
	// 			mapFile.putInt(2045, 66);
	// 			System.out.println("mapFile.getInt(2045):" + mapFile.getInt(2045));
	// 		}
	// 		finally {
	// 			if( null != lock)
	// 				lock.release();
	// 		}
	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
	// }
	
}

