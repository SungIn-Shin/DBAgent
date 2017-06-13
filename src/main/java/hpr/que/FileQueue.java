 package hpr.que;


import hpr.util.DumpBytes;
import hpr.util.Except;
import hpr.util.endian;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Date;

public class FileQueue {


	private class QueHeader {

		public byte[] typeView		= new byte[7];
		public byte[] totalCntView	= new byte[13];
		public int totalCnt;
		public int lastPopTime;
		public short maxFileSize;
		public byte slotSize;
		public short pushSlotIdx;
		public int pushOffset;
		public short popSlotIdx;
		public int popOffset;
		public byte[] slotMap		= new byte[981]; 

		public void setTypeView(String typeView) {

			System.arraycopy(String.format("%6s\n", typeView).getBytes(), 0, this.typeView, 0, 7);
		}

		public void setTotalCnt(int totalCnt) {
			this.totalCnt = totalCnt;

			System.arraycopy(String.format("%12s\n","" + totalCnt).getBytes(), 0, this.totalCntView, 0, 13);
		}


		public void clear() {
			Arrays.fill (typeView		, (byte)0);
			Arrays.fill (totalCntView	, (byte)0);
			totalCnt 		= 0;
			lastPopTime		= 0;
			maxFileSize		= 0;
			slotSize		= 0;
			pushSlotIdx		= 0;
			pushOffset		= 0;
			popSlotIdx		= 0;
			popOffset		= 0;
			Arrays.fill (slotMap		, (byte)0);
		}

		public void decode (ByteBuffer buff) {

			buff.position(0);

			buff.get(typeView);
			buff.get(totalCntView);
			totalCnt 	= endian.swap(buff.getInt());
			lastPopTime = endian.swap(buff.getInt());
			maxFileSize = endian.swap(buff.getShort());
			slotSize	= buff.get();
			pushSlotIdx = endian.swap(buff.getShort());
			pushOffset	= endian.swap(buff.getInt());
			popSlotIdx	= endian.swap(buff.getShort());
			popOffset	= endian.swap(buff.getInt());
			buff.get(slotMap);
		}

		public void encode (ByteBuffer buff) {

			buff.clear();

			buff.put(typeView)
			.put(totalCntView)
			.putInt(endian.swap(totalCnt))
			.putInt(endian.swap(lastPopTime))
			.putShort(endian.swap(maxFileSize))
			.put(slotSize)
			.putShort(endian.swap(pushSlotIdx))
			.putInt(endian.swap(pushOffset))
			.putShort(endian.swap(popSlotIdx))
			.putInt(endian.swap(popOffset))
			.put(slotMap)
			;


			buff.flip();
		}

		@Override
		public String toString() {

			return "QueHeader [typeView=" + (new String(typeView)).replace('\n', ' ') + ", totalCntView="
					+ (new String(totalCntView)).replace('\n', ' ') + ", totalCnt=" + totalCnt
					+ ", lastPopTime=" + lastPopTime + ", maxFileSize="
					+ maxFileSize + ", slotSize=" + slotSize + ", pushSlotIdx="
					+ pushSlotIdx + ", pushOffset=" + pushOffset
					+ ", popSlotIdx=" + popSlotIdx + ", popOffset=" + popOffset
					+ ", slotMap=" + slotMap + "]";
		}



	}	

	static byte SLOT_REDIRECT 	= (byte)'R';
	static byte SLOT_ITEM 		= (byte)'I';

	private class ItemHead {

		public byte type;
		public short value;		
	}



	//	static int MEGA_BYTES = 64;
	static int MEGA_BYTES = 1024 * 1024;

	private ByteBuffer headerBuffer = ByteBuffer.allocateDirect(1024);
	private ByteBuffer itemHeadBuffer = ByteBuffer.allocateDirect(3);
	private ByteBuffer itemBodyBuffer = ByteBuffer.allocateDirect(Short.MAX_VALUE);


	private QueHeader header = new QueHeader();
	private FileChannel fileChannel;

	private String pathName;
	private String fileName;
	private short maxFileSize;
	private byte slotSize;
	
	
	private File fileExist_;
	private RandomAccessFile file_;

	public FileQueue() {
		super();
	}

	public void create (String pathName, String fileName) throws IOException {

		this.create (pathName, fileName, Short.MAX_VALUE, (byte)1);
	}

	public void create (String pathName, String fileName, short maxFileSize, byte slotSize) throws IOException {

		close();

		synchronized (this) {
			
			this.pathName = pathName;
			this.fileName = fileName;
			this.maxFileSize = maxFileSize;
			this.slotSize = slotSize;
			
			if( null == pathName || pathName.equals("")) {
				fileExist_ = new File( fileName );
			}
			else {
				fileExist_ = new File( pathName, fileName );
			}
			if (!fileExist_.createNewFile())
				throw new IOException ("File already exist: " + pathName + fileName);

			file_ = new RandomAccessFile( fileExist_, "rw" );
			fileChannel = file_.getChannel();

			FileLock lock = fileChannel.lock();
	
			try {
				header.setTypeView("JAVA_Q");
				header.setTotalCnt(0);
				header.maxFileSize	= maxFileSize;
				header.slotSize		= slotSize;
				header.pushSlotIdx	= slotAllocate();
	
				writeHeader();
				//System.out.println("create");
			}
			finally {
				try { lock.release(); } catch (IOException ex)	{}
	
			}
		}
	}

	public void open (String pathName, String fileName) throws IOException {

		close();

		synchronized (this) {
			
			this.pathName = pathName;
			this.fileName = fileName;
	
	
			if( null == pathName || pathName.equals("")) {
				fileExist_ = new File( fileName );
			}
			else {
				fileExist_ = new File( pathName, fileName );
			}
			
			if (!fileExist_.exists())
				throw new IOException ("File not found: [" + pathName + " " + fileName + "]");

			file_ = new RandomAccessFile( fileExist_, "rw" );
			fileChannel = file_.getChannel();
	
			FileLock lock = fileChannel.lock();
			try {
				readHeader();
	
				this.maxFileSize = header.maxFileSize;
				this.slotSize = header.slotSize;			
			}
			finally {
				try { lock.release(); } catch (IOException ex)	{}
			}
		}
		//System.out.println("open");
	}

	public void createOpen (String pathName, String fileName) throws IOException {

		this.createOpen(pathName, fileName, Short.MAX_VALUE, (byte)1);
	}

	public void createOpen (String pathName, String fileName, short maxFileSize, byte slotSize) throws IOException {
		try {
			open(pathName, fileName);
			return;
		}
		catch (Exception ex) {}

		create(pathName, fileName, maxFileSize, slotSize);
	}

	public void close () throws IOException {

		synchronized (this) {
			
			headerBuffer.clear();
			header.clear();
	
			if (null != fileChannel) {
	
				FileLock lock = fileChannel.lock();
				try {
					fileChannel.close();
				}
				catch (IOException ex)
				{}
				finally {
					try { lock.release(); } catch (IOException ex)	{}
				}
				fileChannel = null;
			}		
			
			if( null != file_ ) {
				file_.close();
			}
			file_ = null;
		}
	}

	public void push (byte[] data) throws IOException {

		synchronized (this) {
			FileLock lock = fileChannel.lock();
			try {
				if (isFileRemoved()) {
					try { lock.release(); } catch (IOException ex)	{}
	
					this.createOpen(this.pathName, this.fileName, this.maxFileSize, this.slotSize);
	
					lock = fileChannel.lock();
				}
	
				readHeader();
				
				header.lastPopTime = (int)((new Date()).getTime() / 1000);
	
				int spaceNeed = data.length + itemHeadBuffer.capacity() * 2;
				int spaceLeft = header.slotSize * MEGA_BYTES - header.pushOffset;
	
				//System.out.println("[PUSH] need:" + spaceNeed + ", left:" + spaceLeft);
	
				gotoPushPosition();
	
				if (spaceNeed > spaceLeft) {
	
					header.pushSlotIdx = slotAllocate();
					header.pushOffset = 0;
	
					writeItemHeader (SLOT_REDIRECT, header.pushSlotIdx);
	
					gotoPushPosition();
				}
	
				writeItemHeader (SLOT_ITEM, (short)data.length);
				writeItemBody (data);
	
				header.pushOffset += itemHeadBuffer.capacity() + data.length;
				header.setTotalCnt (header.totalCnt + 1);
			}
			finally {
				writeHeader();
	
				try { lock.release(); } catch (IOException ex)	{}
			}
		}
	}

	public byte[] pop () throws IOException {

		synchronized (this) {
			
			FileLock lock = fileChannel.lock();
			try {
	
				if (isFileRemoved()) {
					try { lock.release(); } catch (IOException ex)	{}
	
					this.createOpen(this.pathName, this.fileName, this.maxFileSize, this.slotSize);
	
					lock = fileChannel.lock();
				}
				readHeader ();
	
				if (0 == header.totalCnt) {
					return null;
				}
	
				gotoPopPosition();
				ItemHead itemHead = readItemHeader ();
	
				if (SLOT_REDIRECT == itemHead.type) {
	
					slotFree(header.popSlotIdx);
	
					header.popSlotIdx = itemHead.value;
					header.popOffset = 0;
	
					gotoPopPosition();
					itemHead = readItemHeader ();
				}
	
				itemBodyBuffer.clear();
				itemBodyBuffer.limit(itemHead.value);
				fileChannel.read (itemBodyBuffer);
				itemBodyBuffer.flip();
	
				header.popOffset += itemHeadBuffer.capacity() + itemHead.value;
				header.setTotalCnt (header.totalCnt - 1);
	
				byte [] data = new byte[itemHead.value];
				itemBodyBuffer.get (data);
	
				return data;
			}
			finally {
				writeHeader();
	
				try { lock.release(); } catch (IOException ex)	{}
			}
		}
	}

	private void gotoPushPosition () throws IOException {

		long position = headerBuffer.capacity() + header.pushSlotIdx * header.slotSize * MEGA_BYTES + header.pushOffset;

		fileChannel.position(position);

		//System.out.println("gotoPushPosition() -> " + position);
	}

	private void gotoPopPosition () throws IOException {

		long position = headerBuffer.capacity() + header.popSlotIdx * header.slotSize * MEGA_BYTES + header.popOffset;

		fileChannel.position(position);

		//System.out.println("gotoPopPosition() -> " + position);
	}

	private void writeItemHeader (byte type, short value) throws IOException {

		itemHeadBuffer.clear();
		itemHeadBuffer.put(type);
		itemHeadBuffer.putShort(endian.swap(value));
		itemHeadBuffer.flip();

		fileChannel.write(itemHeadBuffer);

		//System.out.println("write_item_header: (" + (char)(type & 0xFF) + "," + value + ")");
	}

	private void writeItemBody (byte[] data) throws IOException {

		itemBodyBuffer.clear();
		itemBodyBuffer.put(data);
		itemBodyBuffer.flip();

		fileChannel.write (itemBodyBuffer);

		//System.out.println("write_item_body: len(" + data.length + ")");
	}
	 
	private ItemHead readItemHeader () throws IOException {

		itemHeadBuffer.clear();
		fileChannel.read(itemHeadBuffer);
		itemHeadBuffer.flip();

		ItemHead head = new ItemHead();

		head.type = itemHeadBuffer.get ();
		head.value = endian.swap(itemHeadBuffer.getShort());

//		System.out.println("read_item_header: (" + (char)(head.type & 0xFF) + "," + head.value + ")");

		return head;
	}

	private void readHeader () throws IOException {

		headerBuffer.clear();

		fileChannel.position(0);
		fileChannel.read(headerBuffer);

		header.decode(headerBuffer);

		//System.out.println("read_header: " + header);
	}

	private void writeHeader () throws IOException {

		header.encode(headerBuffer);

		fileChannel.position(0);
		fileChannel.write(headerBuffer);

		//System.out.println("write_header: " + header);
	}

	private short slotAllocate() {

		for (int i = 0; i < header.slotMap.length; ++i) {

			byte chunk = header.slotMap[i];
			if (-128 == chunk)
				continue;

			for (byte j=0; j<8; ++j) {

				if (0 == ((chunk >> j) & 1)) {
					chunk |= 1 << j;
					header.slotMap[i] = chunk;

					short slotIdx = (short)((i * 8) + j);

					//System.out.println("slotAllocate:" + slotIdx);
					return slotIdx;
				}
			}
		}
		throw new RuntimeException("Slot fulled"); 
	}

	private void slotFree (short slotIdx) throws IOException {

		if (slotIdx >= header.slotMap.length * 8)
			throw new RuntimeException("Overflow slotIdx:" + slotIdx); 

		int quot = slotIdx / 8;
		int rem = slotIdx % 8;

		header.slotMap[quot] &= ~(1 << rem);

		//System.out.println("slotFree:" + slotIdx);
	}


	private boolean isFileRemoved () throws IOException {

		return !fileExist_.exists();
	}
	

	// public static void main(String[] args) {
	
	// 	System.out.println("젭알!");

	// 	try {
			
	// 		FileQueue msgQueue = new FileQueue();
	// 		msgQueue.createOpen("", "email.que");
			
			
	// 		while (true) {
	// 			byte[] buff = msgQueue.pop();
	// 			if( null == buff) {
	// 				try {
	// 					Thread.sleep(1000);
	// 				} catch (InterruptedException e) {}
	// 				continue;
	// 			}
				
	// 			System.out.println("POP:" + DumpBytes.str(buff));
				
	// 		}

	// 		//Main.makeConfigFile();
	// 		//(new Main4Similar()).start();
	// 	} 
	// 	catch (IOException ex) {
	// 		System.out.println(Except.getStackTrace(ex) );
	// 	}
	// }
}
