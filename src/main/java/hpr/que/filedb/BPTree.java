package hpr.que.filedb;

import hpr.util.Pair;
import hpr.util.Tuple;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Stack;

public class BPTree {

	private final static int MAX_DISPLAY_LENGTH = 30;

	protected final int pageSize_;
	private final int keySize_;
	private final int valueSize_;

	protected class BPTreeHeader {

		public String displayLabel_;
		public int recordCnt_;
		public int recordSubCnt_;
		
		public int pageIdxRoot_;
		public int pageIdxSeqSet_;

		final public int bptreeOrder_;
		final public int bptreeKeyMinCnt_;
		final public int bptreeRecordMaxCnt_;
		final public int bptreeRecordMinCnt_;

		private ByteBuffer buff_ = ByteBuffer.allocate(pageSize_);

		public BPTreeHeader () {
			bptreeOrder_ 		= ((pageSize_ - BPTreePage.HEAD_SIZE - 4) / (keySize_ + 4)) + 1;
			bptreeKeyMinCnt_	= (bptreeOrder_ / 2) - 1 + (bptreeOrder_ % 2);
			bptreeRecordMaxCnt_	= (pageSize_ - BPTreePage.HEAD_SIZE) / (keySize_ + valueSize_);
			bptreeRecordMinCnt_	= bptreeRecordMaxCnt_ / 2;
		}

		public void create() throws IOException {
			String display = String.format("HPR k:%04d v:%04d\r\n", keySize_, valueSize_);
			
			displayLabel_ 	= display.substring(0, Math.min(display.length(), MAX_DISPLAY_LENGTH));
			recordCnt_ 		= 0;
			recordCnt_ 		= 0;
			pageIdxRoot_ 	= diskPageMgr_.alloc();
			pageIdxSeqSet_ 	= pageIdxRoot_;
		}

		public void writeHeader() throws IOException {

			buff_.clear();
			buff_.put( displayLabel_.getBytes() );
			buff_.position( MAX_DISPLAY_LENGTH );
			
			buff_.putInt( recordCnt_ );
			buff_.putInt( recordSubCnt_ );
			buff_.putInt( diskPageMgr_.getPageIdxMax() );
			buff_.putInt( diskPageMgr_.getPageIdxFree() );
			buff_.putInt( pageIdxRoot_ );
			buff_.putInt( pageIdxSeqSet_ );

			buff_.putInt( bptreeOrder_ );
			buff_.putInt( bptreeKeyMinCnt_ );
			buff_.putInt( bptreeRecordMaxCnt_ );
			buff_.putInt( bptreeRecordMinCnt_ );
			
			diskPageMgr_.writePage( 0, 0, buff_.array(), 0, pageSize_ );
		}

		public void readHeader() throws IOException {

			Arrays.fill( buff_.array(), 0, buff_.array().length, (byte)0);
			diskPageMgr_.readPage( 0, 0, buff_.array(), pageSize_ );
		
			buff_.position(0);
			displayLabel_ = new String( buff_.array(), 0, MAX_DISPLAY_LENGTH );
			
			buff_.position (MAX_DISPLAY_LENGTH);

			recordCnt_ 		= buff_.getInt();
			recordSubCnt_ 	= buff_.getInt();
			
			diskPageMgr_.setPageIdxMax( buff_.getInt() );
			diskPageMgr_.setPageIdxFree( buff_.getInt() );

			pageIdxRoot_	= buff_.getInt();
			pageIdxSeqSet_	= buff_.getInt();

			int bptreeOrder			= buff_.getInt();
			int bptreeKeyMinCnt		= buff_.getInt();
			int bptreeRecordMaxCnt	= buff_.getInt();
			int bptreeRecordMinCnt	= buff_.getInt();
			
			if( bptreeOrder != bptreeOrder_
					|| bptreeKeyMinCnt != bptreeKeyMinCnt_
					|| bptreeRecordMaxCnt != bptreeRecordMaxCnt_
					|| bptreeRecordMinCnt != bptreeRecordMinCnt_) {
				throw new RuntimeException("Invalid key or value size with alreay exist file");
			}
				
		}
	}	

	protected class BPTreePage {
		final public static int HEAD_SIZE = 12;
		
		private ByteBuffer 	buff_ = ByteBuffer.allocate( pageSize_ + keySize_ + valueSize_);
		
		public BPTreePage( int pageIdx, boolean isLeafPage ) throws IOException {
			setPageIdx( pageIdx );
			setPageIdxNext( isLeafPage ? 0 : -1 );
			setKeyCnt( 0 );

		}
		
		public BPTreePage( int pageIdx ) throws IOException {
			read( pageIdx );
		}
		
		public StringBuilder display( StringBuilder sb ) throws IOException {
			if( isLeaf() ) {
				sb.append("LEAF");
			}
			else {
				sb.append("NON-LEAF");
			}
			sb.append(" pageIdx:").append(getPageIdx());
			sb.append(" next:").append(getPageIdxNext());
			sb.append(" cnt:").append(getKeyCnt());
			sb.append("\r\n");

			if( isLeaf() ) {
				for( int i = 0; i < getKeyCnt(); ++i ) {
					sb.append("\titem_").append(i).append("[")
						.append(new String(getKey(i))).append(":").append(new String(getValue(i)))
						.append("]");
					sb.append("\r\n");
				}
			}
			else {
				int i = 0;
				for( ; i < getKeyCnt(); ++i ) {
					sb.append("\tchild_").append(i).append(":").append(getChild(i));
					sb.append(" key_").append(i).append("[").append(new String(getKey(i))).append("]");
					sb.append("\r\n");
				}
				sb.append("\tchild_").append(i).append(":").append(getChild(i));
				sb.append("\r\n");
								
			}
			sb.append("\r\n");
			
			if( !isLeaf() ) {
				for( int i = 0; i <= getKeyCnt(); ++i ) {
					BPTreePage page = new BPTreePage( getChild(i) );
					
					page.display(sb);
				}
			
			}
			
			return sb;
		}

		public int getPageIdx() {
			buff_.position(0);
			return buff_.getInt();
		}
		
		public void setPageIdx( int pageIdx ) {
			buff_.position(0);
			buff_.putInt( pageIdx );
		}

		public int getPageIdxNext() {
			buff_.position(4);
			return buff_.getInt();
		}
		
		public void setPageIdxNext( int pageIdxNext ) {
			buff_.position(4);
			buff_.putInt( pageIdxNext );
		}

		public int getKeyCnt() {
			buff_.position(8);
			return buff_.getInt();
		}
		
		public void setKeyCnt( int keyCnt ) {
			buff_.position(8);
			buff_.putInt( keyCnt );
		}
		
		public byte[] getKey( int index ) {
			
			byte [] key = new byte[keySize_];
			if( isLeaf() ) {
				buff_.position( HEAD_SIZE + (index * (keySize_ + valueSize_)) );
			}
			else {
				buff_.position( HEAD_SIZE + (Integer.SIZE / 8) + (index * (keySize_ + (Integer.SIZE / 8))) );
			}
			buff_.get( key, 0, key.length );

			return key;
		}

		public byte[] getValue( int index ) {
			if( !isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for leaf node");
			}
			
			byte [] value = new byte[valueSize_];

			buff_.position( HEAD_SIZE + (index * (keySize_ + valueSize_)) + keySize_ );
			buff_.get( value, 0, value.length);

			return value;
		}
		
		public int getChild( int index ) {
			if( isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for non-leaf node");
			}
			
			buff_.position( HEAD_SIZE + (index * (keySize_ + (Integer.SIZE / 8))) );
			
			return buff_.getInt();
		}
		
		public void putKey( int index, byte[] key ) {
			if( isLeaf() ) {
				int pos = HEAD_SIZE + (index * (keySize_ + valueSize_));
				Arrays.fill( buff_.array(), pos, pos + keySize_, (byte)0);
				System.arraycopy( key, 0, buff_.array(), pos, Math.min(keySize_, key.length));
			}
			else {
				int pos = HEAD_SIZE + (Integer.SIZE / 8) + (index * (keySize_ + (Integer.SIZE / 8)));
				Arrays.fill( buff_.array(), pos, pos + keySize_, (byte)0);
				System.arraycopy( key, 0, buff_.array(), pos, Math.min(keySize_, key.length));
			}
		}
		
		public void putValue( int index, byte[] value ) {
			if( !isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for leaf node");
			}
			int pos = HEAD_SIZE + (index * (keySize_ + valueSize_)) + keySize_;
			Arrays.fill( buff_.array(), pos, pos + valueSize_, (byte)0);

			System.arraycopy( value, 0, buff_.array(), HEAD_SIZE + (index * (keySize_ + valueSize_)) + keySize_, Math.min(valueSize_, value.length));
		}
		
		public void putChild( int index, int pageIdx ) {
			if( isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for non-leaf node");
			}

			buff_.position( HEAD_SIZE + (index * (keySize_ + (Integer.SIZE / 8))) );
			buff_.putInt( pageIdx );
		}
				

		public void write() throws IOException {
			diskPageMgr_.writePage( getPageIdx(), 0, buff_.array(), 0, pageSize_ );
		}

		public void read( int pageIdx ) throws IOException {

			///
			//buff_ = ByteBuffer.allocate( pageSize_ + keySize_ + valueSize_);
			Arrays.fill( buff_.array(), 0, buff_.array().length, (byte)0);
			
			diskPageMgr_.readPage( pageIdx, 0, buff_.array(), pageSize_ );
		}
		
		public boolean isLeaf() {
			return 0 > getPageIdxNext() ? false : true;
		}

		public boolean isFull() {
			if( isLeaf() ) {
				if( getKeyCnt() == mainHeader_.bptreeRecordMaxCnt_ )
					return true;
				else 
					return false;
			}
			else {
				if( getKeyCnt() == mainHeader_.bptreeOrder_ - 1 )
					return true;
				else 
					return false;
			}
		}
		
		public boolean insertLeaf( int index, final byte[] key, final byte[] value ) {
			shift( index, index + 1, getKeyCnt() - index );

			putKey( index, key );
			putValue( index, value );
			
			setKeyCnt( getKeyCnt() + 1 );
			
			return true;
		}
		
		public boolean insertNode( int index, final byte[] key, final int pageIdx ) {
			shift( index, index + 1, getKeyCnt() - index );
			
			putKey( index, key );
			putChild( index + 1, pageIdx );
			
			setKeyCnt( getKeyCnt() + 1 );
			
			return true;
		}
		
		public Pair<Integer, byte[]> splitLeaf( final int index, final byte[] key, final byte[] value ) throws IOException {
			if( !isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for leaf node");
			}
			
			insertLeaf( index, key, value);
			
			int keyCnt = getKeyCnt();
			int midIndex = (keyCnt / 2) - 1 + (keyCnt % 2);
			byte[] midKey = getKey( midIndex );
			
			BPTreePage rightPage = new BPTreePage( diskPageMgr_.alloc(), true );
			
			int pageIdxNext = getPageIdxNext();
			setPageIdxNext( rightPage.getPageIdx() );
			rightPage.setPageIdxNext( pageIdxNext );
			
			rightPage.moveNodes( this, midIndex + 1, keyCnt - midIndex - 1, 0);
			rightPage.write();
			
			trim( midIndex + 1);
			write();
			
			return new Pair<Integer, byte[]>(rightPage.getPageIdx(), midKey);
		}
		
		public Pair<Integer, byte[]> splitNode( final int index, final byte[] key, final int pageIdx ) throws IOException {
			if( isLeaf() ) {
				throw new RuntimeException("Invalid function call: only for non-leaf node");
			}

			insertNode( index, key, pageIdx );
			
			int keyCnt = getKeyCnt();
			int midIndex = keyCnt / 2;
			byte[] midKey = getKey( midIndex );
			
			BPTreePage rightPage = new BPTreePage( diskPageMgr_.alloc(), false );
			
			rightPage.moveNodes( this, midIndex + 1, keyCnt - midIndex - 1, 0);
			rightPage.write();
			
			trim( midIndex );
			write();
			
			return new Pair<Integer, byte[]>(rightPage.getPageIdx(), midKey);
		}
	
		private void moveNodes( BPTreePage src, final int from, final int count, final int to ) {
			if( isLeaf() ) {
				System.arraycopy( src.buff_.array(), HEAD_SIZE + (from * (keySize_ + valueSize_))
								, buff_.array(), HEAD_SIZE + (to * (keySize_ + valueSize_))
								, count * (keySize_ + valueSize_));
			}
			else {
				System.arraycopy( src.buff_.array(), HEAD_SIZE + (from * (keySize_ + (Integer.SIZE / 8)))
								, buff_.array(), HEAD_SIZE + (to * (keySize_ + (Integer.SIZE / 8)))
								, (Integer.SIZE / 8) + count * (keySize_ + (Integer.SIZE / 8)));
			}
			
			setKeyCnt( to + count );
		}
		
		
		private void trim( final int index ) {
			setKeyCnt( index );
			
			int pos = 0;
			if( isLeaf() ) {
				pos = HEAD_SIZE + (index * (keySize_ + valueSize_));
			}
			else {
				pos = HEAD_SIZE + (Integer.SIZE / 8) + (index * (keySize_ + (Integer.SIZE / 8)));
			}
			Arrays.fill(buff_.array(), pos, pageSize_, (byte)0);
		}
		
	
		private void shift( int fromIndex, int toIndex, int count ) {
			if( fromIndex < toIndex ) {
				for( int i = count - 1; i >= 0; --i)
					copyNode( fromIndex + i, toIndex + i );
			}
			else {
				for( int i = 0; i < count; ++i)
					copyNode( fromIndex + i, toIndex + i );
			}
		}
		
		private void copyNode( int fromIndex, int toIndex ) {
			if( isLeaf() ) {
				System.arraycopy( buff_.array(), HEAD_SIZE + (fromIndex * (keySize_ + valueSize_))
								, buff_.array(), HEAD_SIZE + (toIndex * (keySize_ + valueSize_)), (keySize_ + valueSize_));
			}
			else {
				System.arraycopy( buff_.array(), HEAD_SIZE + (Integer.SIZE / 8) + (fromIndex * (keySize_ + (Integer.SIZE / 8)))
								, buff_.array(), HEAD_SIZE + (Integer.SIZE / 8) + (toIndex * (keySize_ + (Integer.SIZE / 8))), (keySize_ + (Integer.SIZE / 8)));
			}
		}
		
		private boolean hasEmptySlot() {
			int minKeyCnt = isLeaf() ? mainHeader_.bptreeRecordMinCnt_ : mainHeader_.bptreeKeyMinCnt_;
			
			if( getKeyCnt() > minKeyCnt) {
				return true;
			}
			else {
				return false;
			}
		}
		
		public void removeLeaf( final int index ) {
			
			shift( index + 1, index, getKeyCnt() - index );
			
			setKeyCnt( getKeyCnt() - 1 );
			
			trim( getKeyCnt() );
		}
		
		public void removeNode( final int index ) {
	
			shift( index + 1, index, getKeyCnt() - index );
			
			trim( getKeyCnt() - 1 );
		}
		
		public int selectSibling( BPTreePage pageSibling, final int index ) throws IOException {
			if( 0 == index ) {
				pageSibling.read( getChild(1));
				
				if( pageSibling.hasEmptySlot())
					return index;
			}
			else if( getKeyCnt() == index ) {
				pageSibling.read( getChild(index-1) );
				
				if( pageSibling.hasEmptySlot())
					return index-1;
			}
			else {
				pageSibling.read( getChild(index+1) );

				if( pageSibling.hasEmptySlot())
					return index;
				
				pageSibling.read( getChild(index-1) );

				if( pageSibling.hasEmptySlot())
					return index-1;
			}
		
			return -1;
		}
		
		public void redistributeLeaf( BPTreePage pageSibling, BPTreePage pageParent, final int index, final boolean isSiblingRight ) throws IOException{
			int moveCnt = (getKeyCnt() + pageSibling.getKeyCnt()) / 2 - getKeyCnt();
			
			if( isSiblingRight )  { // <-- 이동
				moveNodes( pageSibling, 0, moveCnt, getKeyCnt());
				
				pageSibling.shift( moveCnt, 0, pageSibling.getKeyCnt() - moveCnt );
				pageSibling.trim( pageSibling.getKeyCnt() - moveCnt );
				
				byte[] key = getKey( getKeyCnt()-1 );
				pageParent.putKey( index, key );
			}
			else	{ // --> 이돝
				int keyCnt = getKeyCnt();
				
				shift( 0, moveCnt, getKeyCnt());
				moveNodes( pageSibling, pageSibling.getKeyCnt() - moveCnt, moveCnt, 0 );
				setKeyCnt( keyCnt + moveCnt );
				
				pageSibling.trim( pageSibling.getKeyCnt() - moveCnt );
				
				byte[] key = pageSibling.getKey( pageSibling.getKeyCnt() - 1);
				pageParent.putKey( index, key );
			}
			
			write();
			pageSibling.write();
		}
		
		void redistributeNode( BPTreePage pageSibling, BPTreePage pageParent, final int index, final boolean isSiblingRight ) throws IOException {
			int moveCnt = (getKeyCnt() + pageSibling.getKeyCnt()) / 2 - getKeyCnt();
			
			if( isSiblingRight ) {	// <-- 이동
				byte[] key = pageParent.getKey( index );
				
				putKey( getKeyCnt(), key );
				putChild( getKeyCnt()+1, pageSibling.getChild(0) );
				
				moveNodes( pageSibling, 0, moveCnt-1, getKeyCnt()+1 );
				
				key = pageSibling.getKey( moveCnt-1 );
				pageParent.putKey( index, key );
				
				pageSibling.putChild( 0, pageSibling.getChild(moveCnt) );
				pageSibling.shift( moveCnt, 0, pageSibling.getKeyCnt() - moveCnt );
				pageSibling.trim( pageSibling.getKeyCnt() - moveCnt);
			}
			else {	// --> 이동
				int cnt = getKeyCnt();
				
				shift( 0, moveCnt, getKeyCnt() );
				putChild( moveCnt, getChild(0) );
				
				byte[] key = pageParent.getKey(index);
				putKey( moveCnt-1, key );
				
				moveNodes( pageSibling, pageSibling.getKeyCnt() - (moveCnt-1), moveCnt-1, 0 );
				setKeyCnt( cnt + moveCnt );

				key = pageSibling.getKey( pageSibling.getKeyCnt() - moveCnt );
				pageParent.putKey( index, key );
				
				putChild( 0, pageSibling.getChild(pageSibling.getKeyCnt() + 1 - moveCnt));
				
				pageSibling.trim( pageSibling.getKeyCnt() - moveCnt);
			}
			
			write();
			pageSibling.write();
		}
		
		public StackData mergeLeaf( BPTreePage pageSibling, BPTreePage pageParent, StackData stackData) throws IOException {
			if( stackData.index_ == pageParent.getKeyCnt() ) {
				stackData.index_ -= 1;
				pageSibling.read( pageParent.getChild( stackData.index_) );
				
				pageSibling.moveNodes( this, 0, getKeyCnt(), pageSibling.getKeyCnt() );
				pageSibling.setPageIdxNext( getPageIdxNext() );
				
				int pageIdx = getPageIdx();
				
				deepCopy( pageSibling );
				
				write();
				
				diskPageMgr_.dealloc( pageIdx );
			}
			else {
				pageSibling.read( pageParent.getChild(stackData.index_ + 1) );
				
				moveNodes( pageSibling, 0, pageSibling.getKeyCnt(), getKeyCnt() );
				setPageIdxNext( pageSibling.getPageIdxNext() );
				
				write();
				
				diskPageMgr_.dealloc( pageSibling.getPageIdx() );
			}
			
			return stackData;
		}
		
		public StackData mergeNode( BPTreePage pageSibling, BPTreePage pageParent, StackData stackData) throws IOException {

			if( stackData.index_ == pageParent.getKeyCnt() ) {
				stackData.index_ -= 1;
				pageSibling.read( pageParent.getChild( stackData.index_) );
				
				byte[] key = pageParent.getKey( stackData.index_ );
				pageSibling.putKey( pageSibling.getKeyCnt(), key );
				pageSibling.putChild( pageSibling.getKeyCnt()+1, getChild(0));
				pageSibling.moveNodes( this, 0, getKeyCnt(), pageSibling.getKeyCnt()+1 );

				int pageIdx = getPageIdx();
				
				deepCopy( pageSibling );
				
				write();
				
				diskPageMgr_.dealloc( pageIdx );
			}
			else {
				pageSibling.read( pageParent.getChild(stackData.index_ + 1) );
				
				byte[] key = pageParent.getKey( stackData.index_ );
				putKey( getKeyCnt(), key );
				putChild( getKeyCnt()+1, pageSibling.getChild(0));
				moveNodes( pageSibling, 0, pageSibling.getKeyCnt(), getKeyCnt()+1 );
				
				write();
				
				diskPageMgr_.dealloc( pageSibling.getPageIdx() );
			}
			
			return stackData;
		}
		
		private void deepCopy( BPTreePage page) {
			System.arraycopy(page.buff_.array(), 0, buff_.array(), 0, page.buff_.array().length); 
		}
	}
	
	protected static class StackData {
	    public int pageIdx_;
	    public int index_;
	    
	    public StackData( int pageIdx, int index ) {
	    	pageIdx_ = pageIdx;
	    	index_ = index;
	    }
	}

	protected interface IFullScan {
		void scan( byte[] key, byte[] value ) throws IOException;
	}


	private PhysicalFile file_;
	protected DiskPageMgr diskPageMgr_;
	protected BPTreeHeader mainHeader_;

	public enum DiskMode {MEMORY_MAP, DISK}

	protected BPTree( int keySize, int valueSize, DiskMode mode ) {
		this( keySize, valueSize, mode, 1024 );
	}

	protected BPTree( int keySize, int valueSize, DiskMode mode, int pageSize ) {
		
		pageSize_	= pageSize;
		keySize_	= keySize;
		valueSize_	= valueSize;

		if( DiskMode.MEMORY_MAP == mode ) {
			file_ = new MemoryMapedFile();
		}
		else {
			file_ = new DiskFile();
		}
		diskPageMgr_ = new DiskPageMgr( file_, pageSize_ );
		mainHeader_ = new BPTreeHeader();
	}

	protected void open( String pathName, String fileName ) throws IOException {
		file_.open(pathName, fileName);
		mainHeader_.readHeader();
	}
	
	protected void create( String pathName, String fileName ) throws IOException {
		file_.create(pathName, fileName);

		mainHeader_.create();
		mainHeader_.writeHeader();
		
		BPTreePage rootPage = new BPTreePage( mainHeader_.pageIdxRoot_, true);
		rootPage.write();
	}
	
	protected void close() throws IOException {
		file_.close();
	}
	
	protected FileLock getLock() throws IOException {
		return file_.getLock();
	}
	
	protected void releaseLock( FileLock lock ) {
		file_.releaseLock( lock );
	}
	
	protected boolean insertRowid( final byte[] key, final byte[] value, BPTreePage page, Stack<StackData> stack ) throws IOException {
		
		byte[] midKey = null;
		
		int pageIdxRight = 0;
		int index = 0;
		
		boolean res = false;

		while( true ) {
			if( stack.empty() ) {
				int pageIdxRoot = diskPageMgr_.alloc();
				int pageIdxLeft = mainHeader_.pageIdxRoot_;
				
				mainHeader_.pageIdxRoot_ = pageIdxRoot;
				page = new BPTreePage( mainHeader_.pageIdxRoot_, false );
				page.putChild( 0, pageIdxLeft );
				
				index = 0;
			}
			else {
				StackData data = stack.pop();
				index = data.index_;
				
				if( 0 != pageIdxRight ) {
					page.read( data.pageIdx_ );
				}
			}
			
			if( page.isFull() ) {
				if( page.isLeaf() ) {
					Pair<Integer, byte[]> ret = page.splitLeaf( index, key, value );
					
					pageIdxRight = ret.getKey();
					midKey = ret.getValue();
				}
				else {
					Pair<Integer, byte[]> ret = page.splitNode( index, midKey, pageIdxRight );
					
					pageIdxRight = ret.getKey();
					midKey = ret.getValue();
				}
			}
			else {
				if( page.isLeaf() ) {
					res = page.insertLeaf( index, key, value );
				}
				else {
					res = page.insertNode( index, midKey, pageIdxRight );
				}
				break;
			}
		}
		
		if( res ) {
			page.write();
		}
		
		mainHeader_.recordCnt_ += 1;
		mainHeader_.writeHeader();
		
		return true;
			
	}
	
	protected void deleteRowid( final byte[] key, BPTreePage page, Stack<StackData> stack ) throws IOException {
		
		BPTreePage pageParent = new BPTreePage( mainHeader_.pageIdxRoot_ );
		BPTreePage pageSibling = new BPTreePage( mainHeader_.pageIdxRoot_ );

		while( true ) {
			StackData data = stack.pop();

			if( page.isLeaf() ) {
				page.removeLeaf( data.index_ );
			}
			else {
				page.removeNode( data.index_ );
			}
			
			if( data.pageIdx_ == mainHeader_.pageIdxRoot_) {
				if( 0 == page.getKeyCnt() && !page.isLeaf()) {
					mainHeader_.pageIdxRoot_ = page.getChild(0);
					
					diskPageMgr_.dealloc( page.getPageIdx() );
					
					mainHeader_.recordCnt_ -= 1;
					mainHeader_.writeHeader();

					return;
				}
				break;
			}
			else if( page.getKeyCnt() 
					>= (page.isLeaf() ? mainHeader_.bptreeRecordMinCnt_ : mainHeader_.bptreeKeyMinCnt_)) {
				break;		
			}
			else {
				data = stack.peek();
				
				pageParent.read( data.pageIdx_ );
				int index = pageParent.selectSibling( pageSibling, data.index_ );
				
				if( -1 == index ) {
					if( page.isLeaf() ) {
						page.mergeLeaf( pageSibling, pageParent, data );
//						data = page.mergeLeaf( pageSibling, pageParent, data );
					}
					else {
						page.mergeNode( pageSibling, pageParent, data );
//						data = page.mergeNode( pageSibling, pageParent, data );
					}
				}
				else {
					if( page.isLeaf() ) {
						page.redistributeLeaf( pageSibling, pageParent, index, (index == data.index_ ? true : false));
					}
					else {
						page.redistributeNode( pageSibling, pageParent, index, (index == data.index_ ? true : false));
					}
				}

				page.deepCopy( pageParent );
				
				if( -1 != index ) {
					break;
				}
			} 
		}
		
		page.write();
		
		mainHeader_.recordCnt_ -= 1;
		mainHeader_.writeHeader();
		return;
		
	}
	
	protected Tuple<Boolean, BPTreePage, Stack<StackData>> lookUpNode( final byte[] findKey ) throws IOException {
		
		Stack<StackData> stack = new Stack<StackData>();
		BPTreePage page = new BPTreePage( mainHeader_.pageIdxRoot_ );
		
		ByteBuffer targetKey = ByteBuffer.allocate( keySize_ );
		targetKey.put( findKey );
		targetKey.clear();

		while( !page.isLeaf() ) {
			int index = 0;
			
			int keyCnt = page.getKeyCnt();
			while( index < keyCnt ) {
				byte[] key = page.getKey( index );
				
				targetKey.clear();
				if (0 > ByteBuffer.wrap(key).compareTo( targetKey ))
					++index;
				else
					break;
			}
			stack.push(new StackData(page.getPageIdx(), index));
			
			page.read( page.getChild( index ) );
//			System.out.println("CHILD:" + index);
//			System.out.println(DumpBytes.str( page.buff_.array(), pageSize_ ));
		}
		
		int index = 0;
		int keyCnt = page.getKeyCnt();
		while( index < keyCnt ) {
			byte[] key = page.getKey( index );
			
			targetKey.clear();
			if( 0 > ByteBuffer.wrap(key).compareTo( targetKey ) )
				++index;
			else
				break;
		}

		stack.push(new StackData(page.getPageIdx(), index));
		if( index < keyCnt ) {
			byte[] key = page.getKey( index );
		
			targetKey.clear();
			if( 0 == ByteBuffer.wrap(key).compareTo( targetKey ) ) {
				return new Tuple<Boolean, BPTreePage, Stack<StackData>>(true, page, stack);
			}
		}
		
		return new Tuple<Boolean, BPTreePage, Stack<StackData>>(false, page, stack);
	}
	

	protected void fullScan( IFullScan func ) throws IOException {
		
		int pageIdx = mainHeader_.pageIdxSeqSet_;

		while( pageIdx > 0) {
			
			BPTreePage page = new BPTreePage( pageIdx );

			int keyCnt = page.getKeyCnt();
			for( int i = 0; i < keyCnt; ++i ) {
				byte[] key = page.getKey( i );
				byte[] value = page.getValue( i );

				func.scan( key, value );
			}
			
			pageIdx = page.getPageIdxNext();
		}
	}
	

}
