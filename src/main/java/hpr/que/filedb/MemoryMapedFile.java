package hpr.que.filedb;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class MemoryMapedFile extends PhysicalFile {

	private final static int SIZE_100MBytes = 1024 * 1024 * 100;
	private MappedByteBuffer byteBuffer_;
	
	protected void fileOpen (String pathName, String fileName, boolean isOpen) throws IOException {
		super.fileOpen(pathName, fileName, isOpen);

		mappingDiskToMemory();
	}
	
	private void mappingDiskToMemory () throws IOException {
		
		int size = 1024; 
		while ( size < super.fileChannel_.size() ) {
			if( SIZE_100MBytes * 5 > size) {
				size *= 2;
			}
			else {
				size += SIZE_100MBytes;
			}
		}
		byteBuffer_ = super.fileChannel_.map( FileChannel.MapMode.READ_WRITE, 0, size );
	}

	
	private void checkAndRemappingDiskToMemory (long length) throws IOException {
		int currLen = byteBuffer_.capacity();
		while( length > currLen ) {
			if( SIZE_100MBytes * 5 > currLen) {
				currLen *= 2;
			}
			else {
				currLen += SIZE_100MBytes;
			}
		}

		if( currLen != byteBuffer_.capacity() ) {
			byteBuffer_ = super.fileChannel_.map(FileChannel.MapMode.READ_WRITE, 0, currLen);
		}
	}

	public void get( long position, byte[] dst, int length ) throws IOException {
	
		checkAndRemappingDiskToMemory( position + length );

		byteBuffer_.position((int)position);
		byteBuffer_.get( dst, 0, length );
	}
	
	public void put( long position, byte[] src, int offset, int length ) throws IOException {

		checkAndRemappingDiskToMemory( position + length );

		byteBuffer_.position((int)position);
		byteBuffer_.put( src, offset, length );
	}
	
	public int getInt( long position ) throws IOException {

		checkAndRemappingDiskToMemory( position + (Integer.SIZE / 8) );

		byteBuffer_.position( (int)position );

		return byteBuffer_.getInt();
	}

	public void putInt( long position, int value ) throws IOException {

		checkAndRemappingDiskToMemory( position + (Integer.SIZE / 8) );

		byteBuffer_.position( (int)position );
		byteBuffer_.putInt( value );
	}
	
	// public static void main(String[] args) {
	
	// 	MemoryMapedFile mapFile = new MemoryMapedFile();
		
	// 	try {
	// 		mapFile.createOpen("aaa1.txt");
			
	// 		FileLock lock = null;
	// 		try {
	// 			lock = mapFile.getLock();
				
	// 			mapFile.putInt(2045, 66);
	// 			System.out.println("mapFile.getInt(2045):" + mapFile.getInt(12045));
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

