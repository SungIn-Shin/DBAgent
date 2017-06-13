package hpr.que.bptree;

import hpr.que.filedb.BPTree;
import hpr.que.filedb.BPTree.DiskMode;
import hpr.util.DumpBytes;
import hpr.util.Pair;
import hpr.util.SizeOf;
import hpr.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class IndexQueue extends BPTree {
	

	private static class QueueHeader {
	
		private static final int SIZE = SizeOf.sizeof(QueueHeader.class);

		private int pageIdxPush_;
		private int pageIdxPop_;
		private int offsetPush_;
		private int offsetPop_;
		private int count_;
//		private long lastPopTime_;
		
		private ByteBuffer buff_ = ByteBuffer.allocate(SIZE);
		
		private void buildPacket() {
			buff_.clear();
			buff_.putInt(pageIdxPush_);
			buff_.putInt(pageIdxPop_);
			buff_.putInt(offsetPush_);
			buff_.putInt(offsetPop_);
			buff_.putInt(count_);
		}			
		
		private void parsePacket( byte[] packet ) {
			if( packet.length > buff_.array().length )
				throw new RuntimeException();
			
			System.arraycopy( packet, 0, buff_.array(), 0, buff_.array().length);
			
			parsePacket();
		}
		
		private void parsePacket() {
			buff_.clear();
			pageIdxPush_= buff_.getInt();
			pageIdxPop_	= buff_.getInt();
			offsetPush_	= buff_.getInt();
			offsetPop_	= buff_.getInt();
			count_		= buff_.getInt();
		}
		
		private String display() {
			StringBuilder sb = new StringBuilder();
			
			sb.append("pageIdxPush_: ").append(pageIdxPush_);
			sb.append(" pageIdxPop_: ").append(pageIdxPop_);
			sb.append(" offsetPush_: ").append(offsetPush_);
			sb.append(" offsetPop_: ").append(offsetPop_);
			sb.append(" count_: ").append(count_);
			
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
	
	public IndexQueue( int keySize, DiskMode mode ) {
		super( keySize, QueueHeader.SIZE, mode );
	}

	public IndexQueue( int keySize, DiskMode mode, int pageSize ) {
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

	public synchronized int keyCount() throws IOException {
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

	public synchronized int subCount() throws IOException {
		if( !isOpened_ )
			return -1;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();

			return mainHeader_.recordSubCnt_;
		}
		finally {
			releaseLock( lock );
		}
	}


	public synchronized ArrayList<Pair<byte[], Integer>> keyAndSubCount() throws IOException {
		
		final class FullScan implements IFullScan {

			ArrayList<Pair<byte[], Integer>> value_ = new ArrayList<Pair<byte[], Integer>>();
			QueueHeader header = new QueueHeader();

			@Override
			public void scan(byte[] key, byte[] value) {
				
				header.parsePacket(value);
				
				//System.out.println("k:" + new String(key) + " v:" + header.count_);
				
				value_.add(new Pair<byte[], Integer>(key, header.count_));
			}
			
		}

		FullScan fullScan = new FullScan();
		
		if( !isOpened_ )
			return fullScan.value_;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();

			super.fullScan( fullScan );
		}
		finally {
			releaseLock( lock );
		}
		
		return fullScan.value_;
	}


	public void push( byte[] key, byte[] value ) throws IOException {
		push( key, value, value.length );
	}
	
	public synchronized void push( byte[] key, byte[] value, int length ) throws IOException {

		if( !isOpened_ )
			return;
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( !ret.get1() ) {
				QueueHeader header = new QueueHeader();

				header.pageIdxPush_ = super.diskPageMgr_.alloc();
				header.pageIdxPop_ = header.pageIdxPush_;
				header.buildPacket();
		//		System.out.println("alloc:" + header.pageIdxPush_);

		//		System.out.println("ready to insert:" + header.display());
				
				super.insertRowid( key, header.buff_.array(), ret.get2(), ret.get3() );

				ret = lookUpNode( key );
				if( !ret.get1() ) {
					throw new RuntimeException();
				}
			}
			
			BPTreePage page = ret.get2();
			int valueIndex = ret.get3().pop().index_;
			
			QueueHeader header = new QueueHeader();
			header.parsePacket(page.getValue( valueIndex ));
			
	//		System.out.println("ready to push:" + header.display());
			pushQue( header, value, length );
			
			page.putValue( valueIndex, header.buff_.array());
			page.write();

			mainHeader_.recordSubCnt_ += 1;
			mainHeader_.writeHeader();
		}
		finally {
			releaseLock( lock );
		}
	}
	
	public synchronized byte[] pop ( final byte[] key ) throws IOException {
	
		if( !isOpened_ )
			return new byte[0];

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
				
			if( !ret.get1() ) {
		//		System.out.println("nothing");
				return new byte[0];
			}
			
			BPTreePage page = ret.get2();
			int valueIndex = ret.get3().peek().index_;
			
			QueueHeader header = new QueueHeader();
			header.parsePacket(page.getValue( valueIndex ));
			
	//		System.out.println("ready to pop:" + header.display());
			byte[] res = popQue( header );
			
			if( header.count_ > 0) {
	//			System.out.println("" + header.count_);

				page.putValue( valueIndex, header.buff_.array());
				page.write();
			}
			else {
	//			System.out.println("dealloc:" + header.pageIdxPop_);
				super.diskPageMgr_.dealloc(header.pageIdxPop_);
				deleteRowid( key, page, ret.get3() );
			}
			
			mainHeader_.recordSubCnt_ -= 1;
			mainHeader_.writeHeader();
			
			return res;
		}
		finally {
			releaseLock( lock );
		}
	}
	
	private void pushQue( QueueHeader header, byte[] value, int length ) throws IOException {
		
		RecordHeader recordHeader = new RecordHeader();
		byte[] recordPacket = recordHeader.buff_.array();
		
		int pageIdx = header.pageIdxPush_;
		int offset = header.offsetPush_;
		
		int currPos = 0;
		//System.out.println("ready to pushQue: pageIdx(" + pageIdx + ") offset(" + offset + ") length:" + length);

		while( length >= 0 ) {
			
			int spaceLeft = super.pageSize_ - offset;

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
	//		System.out.println(" B alloc:" + recordHeader.nextPageIdx_ );

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
			
		header.pageIdxPush_ = pageIdx;
		header.offsetPush_ = offset;
		header.count_ += 1;
		header.buildPacket();
	}

	private byte[] popQue( QueueHeader header ) throws IOException {
	
		ArrayList<byte[]> buffs = new ArrayList<byte[]>();
		
		int pageIdx = header.pageIdxPop_;
		int offset = header.offsetPop_;
		
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
				byte[] buff = new byte[length];
				super.diskPageMgr_.readPage( pageIdx, offset, buff, buff.length );
				offset += buff.length;
				totalSize += buff.length;
			//	System.out.println("pop body:" + recordHeader.display() + " buff:" + new String(buff));

				buffs.add( buff );
			}

			if( 'S' == head ) {
				break;
			}

			if( 'M' != head ) {
				throw new RuntimeException("Invalid head:" + head);
			}
		//	System.out.println(" B dealloc:" + pageIdx);

			super.diskPageMgr_.dealloc(pageIdx);
			
			pageIdx = recordHeader.nextPageIdx_;
			offset = 0;
		}

		header.pageIdxPop_ = pageIdx;
		header.offsetPop_ = offset;
		header.count_ -= 1;
		header.buildPacket();
	//	System.out.println("poped:" + header.display() + " totalSize:" + totalSize);

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

		IndexQueue db = new IndexQueue(80, DiskMode.MEMORY_MAP);
//		IndexQueue db = new IndexQueue(80, DiskMode.DISK);
		try {

			File f = new File("db1.txt");
			f.delete();
			
			db.createOpen (null, "db1.txt");
			
			long start = System.currentTimeMillis();

			
			int cnt = 139;
			
			ByteBuffer b = ByteBuffer.allocate(cnt*cnt*100);

			for( int i = 1; i < cnt; ++i) {
				int target = i*i*100;
				for(int j =0; j < target; ++j) {
					b.put(j, (byte)i);
				}
				
				db.push("ABC".getBytes(),b.array(), target);
				System.out.println("PUSH: " + i + " length:" +target + " cnt:" + db.subCount());

			}
		
			long end = System.currentTimeMillis();
			System.out.println( "실행 시간 : " + ( end - start )/1000.0 );

			start = System.currentTimeMillis();

			db.keyAndSubCount();
			
			for( int i = 1; i < cnt; ++i) {
				
				byte[] res = db.pop("ABC".getBytes());
				if( res.length == 0 ) {
					System.out.println("헐! 없어?" + i);
					System.exit(1);
				}
				
				System.out.println("POP: " + i + " length:" + res.length + " cnt:" + db.subCount());
			
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
	
	// public static void main(String[] args) {

	// 	func1();
	// }
	
	
	
}
