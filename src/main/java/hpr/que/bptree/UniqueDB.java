package hpr.que.bptree;

import hpr.que.filedb.BPTree;
import hpr.util.DumpBytes;
import hpr.util.Pair;
import hpr.util.SizeOf;
import hpr.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Stack;

public class UniqueDB extends BPTree {
	

	private static class QueueHeader {
	
		private static final int SIZE = SizeOf.sizeof(QueueHeader.class);

		private int pageIdx_;
		
		private ByteBuffer buff_ = ByteBuffer.allocate(SIZE);
		
		private void buildPacket() {
			buff_.clear();
			buff_.putInt(pageIdx_);
		}			
		
		private void parsePacket( byte[] packet ) {
			if( packet.length > buff_.array().length )
				throw new RuntimeException();
			
			System.arraycopy( packet, 0, buff_.array(), 0, buff_.array().length);
			
			parsePacket();
		}
		
		private void parsePacket() {
			buff_.clear();
			pageIdx_= buff_.getInt();
		}
		
		private String display() {
			StringBuilder sb = new StringBuilder();
			
			sb.append("pageIdx_: ").append(pageIdx_);
			
			return sb.toString();
		}
	}
	
	private static class RecordHeader {
		private static final int SIZE = SizeOf.sizeof(RecordHeader.class);

		private char head_;
		private int length_;
		private int nextPageIdx_;

		private ByteBuffer buff_ = ByteBuffer.allocate(SIZE);
		
		private void buildPacket() {
			buff_.clear();
			buff_.putChar(head_);
			buff_.putInt(length_);
			buff_.putInt(nextPageIdx_);
		}			
		
		private void parsePacket( byte[] packet ) {
			if( packet.length > buff_.array().length )
				throw new RuntimeException();
			
			System.arraycopy( packet, 0, buff_.array(), 0, buff_.array().length);
			parsePacket();
		}
		
		private void parsePacket() {
			buff_.clear();
			head_		= buff_.getChar();
			length_		= buff_.getInt();
			nextPageIdx_= buff_.getInt();
		}
		
		private String display() {
			StringBuilder sb = new StringBuilder();
			
			sb.append("head_: ").append(head_);
			sb.append(" length_: ").append(length_);
			sb.append(" nextPageIdx_: ").append(nextPageIdx_);
			
			return sb.toString();
		}		
	}
	
	private boolean isOpened_ = false;
	
	public UniqueDB( int keySize, DiskMode mode ) {
		super( keySize, QueueHeader.SIZE, mode );
	}

	public UniqueDB( int keySize, DiskMode mode, int pageSize ) {
		super( keySize, QueueHeader.SIZE, mode, pageSize );
	}

	public synchronized void createOpen( String pathName, String fileName ) throws IOException {
		
		if( isOpened_ )
			return;
		
		try {
			open( pathName, fileName );
			isOpened_ = true;
			return;
		}
		catch (IOException ex) {}

		create( pathName, fileName );
		isOpened_ = true;
		
	}
	
	public synchronized void close() throws IOException {
		super.close();
		
		isOpened_ = false;
	}
	
	public synchronized int count() throws IOException {
		if( !isOpened_ )
			return -1;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();

			return mainHeader_.recordCnt_;
		}
		finally {
			releaseLock( lock );
		}
	}
	
	public synchronized void fullScan( IFullScan callback ) throws IOException {

		final class FullScan implements IFullScan {

			IFullScan callback_;
			
			public FullScan( IFullScan callback ) {
				callback_ = callback;
			}
			
			@Override
			public void scan(byte[] key, byte[] value) throws IOException {
				
				QueueHeader header = new QueueHeader();
				header.parsePacket(value);
				
				byte[] res = retributeRecord( header, true, false);
				callback_.scan( key, res );
			}
		}
		
		if( !isOpened_ )
			return;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();

			super.fullScan( new FullScan(callback) );
		}
		finally {
			releaseLock( lock );
		}
	}

	public synchronized byte[] select ( final byte[] key ) throws IOException {
		return retribute ( key, true, false );
	}
	
	public synchronized void delete ( final byte[] key ) throws IOException {
		retribute ( key, false, true );
	}
	
	public synchronized byte[] selectAndDelete ( final byte[] key ) throws IOException {
		return retribute ( key, true, true );
	}
	
	public boolean insert( byte[] key, byte[] value ) throws IOException {
		return insert( key, value, value.length );
	}
	
	public boolean update( byte[] key, byte[] value ) throws IOException {
		return update( key, value, value.length );
	}

	public synchronized boolean update( byte[] key, byte[] value, int length ) throws IOException {
		
		if( !isOpened_ )
			return false;

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
				
			if( !ret.get1() ) {
				return false;
			}			
			
			BPTreePage page = ret.get2();
			int valueIndex = ret.get3().peek().index_;
			
			QueueHeader header = new QueueHeader();
			header.parsePacket(page.getValue( valueIndex ));
			
			retributeRecord( header, false, true );
			
			header.pageIdx_ = super.diskPageMgr_.alloc();
			header.buildPacket();
//			System.out.println("alloc:" + header.pageIdx_);
			
			insertRecord( header, value, length );

			mainHeader_.writeHeader();		
		}
		finally {
			releaseLock( lock );
		}
		return true;
	}
	
	public synchronized boolean insert( byte[] key, byte[] value, int length ) throws IOException {

		if( !isOpened_ )
			return false;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( ret.get1() ) {
				return false;
			}
			
			QueueHeader header = new QueueHeader();

			header.pageIdx_ = super.diskPageMgr_.alloc();
			header.buildPacket();
//			System.out.println("alloc:" + header.pageIdx_);

	//		System.out.println("ready to insert:" + header.display());
			
			super.insertRowid( key, header.buff_.array(), ret.get2(), ret.get3() );

			ret = lookUpNode( key );
			if( !ret.get1() ) {
				throw new RuntimeException();
			}
			
			BPTreePage page = ret.get2();
			int valueIndex = ret.get3().pop().index_;
			
			header.parsePacket(page.getValue( valueIndex ));
			
	//		System.out.println("ready to push:" + header.display());
			insertRecord( header, value, length );

			mainHeader_.writeHeader();
		}
		finally {
			releaseLock( lock );
		}
		
		return true;
	}
	
	private byte[] retribute ( final byte[] key, boolean isCopy, boolean isDelete ) throws IOException {
		
		if( !isOpened_ )
			return new byte[0];

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
				
			if( !ret.get1() ) {
				return new byte[0];
			}
			
			BPTreePage page = ret.get2();
			int valueIndex = ret.get3().peek().index_;
			
			QueueHeader header = new QueueHeader();
			header.parsePacket(page.getValue( valueIndex ));
			
	//		System.out.println("ready to pop:" + header.display());
			byte[] res = retributeRecord( header, isCopy, isDelete );

			if( isDelete ) {
//				System.out.println("delete row:");
	
				super.deleteRowid( key, page, ret.get3() );
			}
			
			return res;
		}
		finally {
			releaseLock( lock );
		}
	}
	
	private void insertRecord( QueueHeader header, byte[] value, int length ) throws IOException {
		
		RecordHeader recordHeader = new RecordHeader();
		byte[] recordPacket = recordHeader.buff_.array();
		
		int pageIdx = header.pageIdx_;
		int offset = 0;
		
		int currPos = 0;
		//System.out.println("ready to pushQue: pageIdx(" + pageIdx + ") offset(" + offset + ") length:" + length);

		while( length >= 0 ) {
			
			int spaceLeft = super.pageSize_;

			if( spaceLeft > RecordHeader.SIZE * 2 + length ) {
				recordHeader.head_ = 'S';
				recordHeader.length_ = length;
				recordHeader.nextPageIdx_ = -1;
				recordHeader.buildPacket();
				
				super.diskPageMgr_.writePage( pageIdx, offset, recordPacket, 0, recordPacket.length);
				offset += recordPacket.length;
				
				super.diskPageMgr_.writePage(pageIdx, offset, value, currPos, recordHeader.length_);
				offset += recordHeader.length_;
				currPos += recordHeader.length_;
				length -= recordHeader.length_;

		//		System.out.println("Signle to pushQue: currPos(" + currPos + ") length(" + length + ") offset(" + offset+ ")");
				break;				
			}

			recordHeader.head_ = 'M';
			recordHeader.length_ = Math.min( spaceLeft - RecordHeader.SIZE, length);
			recordHeader.nextPageIdx_ = diskPageMgr_.alloc();
			recordHeader.buildPacket();
//			System.out.println("-- page alloc:" + recordHeader.nextPageIdx_ );

			super.diskPageMgr_.writePage( pageIdx, offset, recordPacket, 0, recordPacket.length);
			offset += recordPacket.length;
			if( recordHeader.length_ > 0 )
				super.diskPageMgr_.writePage( pageIdx, offset, value, currPos, recordHeader.length_);
			currPos += recordHeader.length_;
			length -= recordHeader.length_;
			
			pageIdx = recordHeader.nextPageIdx_;
			offset = 0;
			
	//		System.out.println("Multi to pushQue: currPos(" + currPos + ") length(" + length + ") offset(" + offset+ ")");
		}
	}

	private byte[] retributeRecord( QueueHeader header, boolean isCopy, boolean isDelete ) throws IOException {
	
		ArrayList<byte[]> buffs = new ArrayList<byte[]>();
		
		int pageIdx = header.pageIdx_;
		int offset = 0;
		
		RecordHeader recordHeader = new RecordHeader();
		byte[] recordPacket = recordHeader.buff_.array();

		int totalSize = 0;
		while( true ) {
		//	System.out.println("ready to pop pageIdx:" + pageIdx + " offset:" + offset);

			super.diskPageMgr_.readPage( pageIdx, offset, recordPacket, recordPacket.length );
			offset += recordPacket.length;
			
			recordHeader.parsePacket();
			char head = recordHeader.head_;
			int length = recordHeader.length_;
			
		//	System.out.println("pop head:" + recordHeader.display() + " offset:" + offset);
			if( length > 0 ) {
				if( isCopy ) {
					byte[] buff = new byte[length];
					super.diskPageMgr_.readPage( pageIdx, offset, buff, buff.length );
					buffs.add( buff );
				}
				offset += length;
				totalSize += length;
			//	System.out.println("pop body:" + recordHeader.display() + " buff:" + new String(buff));

			}

			if( isDelete ) {
//				System.out.println("-- page dealloc:" + pageIdx);
				super.diskPageMgr_.dealloc(pageIdx);
			}

			if( 'S' == head ) {
				break;
			}

			if( 'M' != head ) {
				throw new RuntimeException("Invalid head:" + head);
			}

			pageIdx = recordHeader.nextPageIdx_;
			offset = 0;
		}

	//	System.out.println("poped:" + header.display() + " totalSize:" + totalSize);
		if(! isCopy ) {
			return new byte[0];
		}
		
		int currPos = 0;
		byte[] res = new byte[totalSize];
		for( byte[] buff : buffs ) {

			System.arraycopy( buff, 0, res, currPos, buff.length);
			currPos += buff.length;
		}
	//	System.out.println("res:" + DumpBytes.str(res));
		return res;
	}
	

	private static void func1() {

		UniqueDB db = new UniqueDB(70, DiskMode.MEMORY_MAP);
		try {
			File f = new File("unique_db1.txt");
			f.delete();

			db.createOpen (null, "unique_db1.txt");
			
			boolean b = db.insert("AB1C".getBytes(), "ssssss".getBytes());
			if( !b ) {
				System.out.println("Failed to insert");
			}
			else {
				System.out.println("insert ok");
			}
			b = db.insert("AB1C".getBytes(), "ssss1111ss".getBytes());
			if( !b ) {
				System.out.println("Already exist OK");
			}
			else {
				System.out.println("insert fuck");
			}
				
			byte[] data = db.select("AB1C".getBytes());
			if( data.length == 0 ) {
				System.out.println("Nothing");
			}
			else {
				System.out.println("Found:" + new String(data));
			}
			db.delete("AB1C".getBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void func2() {

//		UniqueDB db = new UniqueDB(90, DiskMode.DISK);
		UniqueDB db = new UniqueDB(90, DiskMode.MEMORY_MAP);
		try {
			File f = new File("unique_db2.txt");
			f.delete();

			db.createOpen (null, "unique_db2.txt");
			
			int cnt = 200;
			ByteBuffer b = ByteBuffer.allocate(cnt*cnt*100);

			long start = System.currentTimeMillis();

			for( int i = 1; i < cnt; ++i) {
				int target = i*i*100;
				for(int j =0; j < target; ++j) {
					b.put(j, (byte)i);
				}
				db.insert(String.format("ABC_%d",i).getBytes(), b.array(), target);
				System.out.println("PUSH: " + i + " length:" + target);
			}
			
			long end = System.currentTimeMillis();
			System.out.println( "실행 시간 : " + ( end - start )/1000.0 );

			start = System.currentTimeMillis();

			for( int i = 1; i < cnt; ++i) {
				byte[] res = db.selectAndDelete(String.format("ABC_%d",i).getBytes());
				if( res.length == 0 ) {
					System.out.println("헐! 없어?: " + i);
					System.exit(1);
				}

				System.out.println("POP: " + i + " length:" + res.length);

				int target = i*i*100;
				for(int j =0; j < target; ++j) {
					if( res[j] != (byte)i) { 
						System.out.println("헐!" + i);
						System.out.println("res:" + new String(res));
						System.exit(1);
					}				
				}
			}
			
			end = System.currentTimeMillis();
			System.out.println( "실행 시간 : " + ( end - start )/1000.0 );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	private static void func3() {

		UniqueDB db = new UniqueDB(70, DiskMode.MEMORY_MAP);
		try {
			//File f = new File("unique_db3.txt");
			//f.delete();

			db.createOpen (null, "unique_db3.txt");
			
			boolean b = db.update("AB1C".getBytes(), "ssssss".getBytes());
			if( !b ) {
				System.out.println("Ok. nothing to update");
			}
			else {
				System.out.println("Fuck!");
			}

			b = db.insert("AB1C".getBytes(), "ssssss".getBytes());
			if( !b ) {
				System.out.println("Failed to insert");
			}
			else {
				System.out.println("insert ok");
			}
				
			b = db.update("AB1C".getBytes(), "FFFFF".getBytes());
			if( !b ) {
				System.out.println("FUCK !");
			}
			else {
				System.out.println("OK update");
			}

			byte[] data = db.select("AB1C".getBytes());
			if( data.length == 0 ) {
				System.out.println("Nothing");
			}
			else {
				System.out.println("Found:" + new String(data));
			}
			db.delete("AB1C".getBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	

	private static void func4() {
		
		class SampleVO implements Serializable {
			public int a;
			public String b;
		}

		UniqueDB db = new UniqueDB(120, DiskMode.MEMORY_MAP);
		try {
			File f = new File("unique_db4.txt");
			f.delete();

			db.createOpen (null, "unique_db4.txt");
			
			SampleVO vo = new SampleVO();
			vo.a  = 4334;
			vo.b = "sssdssssssfsfsdf";
			
			
			hpr.util.Serialize seri = new hpr.util.Serialize();
			byte[] m = seri.marshalling((Object)vo);
			seri.close();
			
			boolean b = db.insert("AB1C".getBytes(), m);
			if( !b ) {
				System.out.println("Failed to insert");
			}
			else {
				System.out.println("insert ok");
			}
				
			byte[] data = db.select("AB1C".getBytes());
			if( data.length == 0 ) {
				System.out.println("Nothing");
			}
			else {
				SampleVO a = (SampleVO)seri.unmarshalling(data);

				System.out.println("Found:" + a.a + " " + a.b);
				
				seri.close();
			}
			db.delete("AB1C".getBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void func5() {
		
		class SampleVO implements Serializable {
			public int a_;
			public int b_;
			public String c_;
			
			public SampleVO( int a, int b, String c) {
				a_ = a;
				b_ = b;
				c_ = c;
			}
		}
		
		final class FullScan implements IFullScan {

			hpr.util.Serialize seri = new hpr.util.Serialize();
			@Override
			public void scan(byte[] key, byte[] value) {
				SampleVO a;
				try {
					a = (SampleVO)seri.unmarshalling(value);
					System.out.println( "K:" + new String(key) + " vo:" + a.a_ +" b:"+ a.b_ + " c:" + a.c_ );
					seri.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			
		}
		
		UniqueDB db = new UniqueDB(70, DiskMode.DISK);
//		UniqueDB db = new UniqueDB(70, DiskMode.MEMORY_MAP);
		try {
			File f = new File("unique_db5.txt");
			f.delete();

			db.createOpen (null, "unique_db5.txt");
			
			hpr.util.Serialize seri = new hpr.util.Serialize();
			
			SampleVO aaa = new SampleVO(1,2,"A1");
			byte[] m = seri.marshalling((Object)aaa);
			db.insert( "A1".getBytes(), m);
			seri.close();
			
			db.insert( "A2".getBytes(), seri.marshalling((Object)(new SampleVO(2,4,"A2"))));
			seri.close();
			
			db.insert( "A3".getBytes(), seri.marshalling((Object)(new SampleVO(3,6,"A3"))));
			seri.close();
				
			FullScan fs = new FullScan();
			db.fullScan(fs);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	// public static void main(String[] args) {

	// 	func5();
	// }
	
	
	
}
