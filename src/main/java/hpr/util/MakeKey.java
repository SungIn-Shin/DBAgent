package hpr.util;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;

import hpr.que.filedb.MemoryMapedFile;
import hpr.que.filedb.PhysicalFile;

public class MakeKey {
	
	public static interface IMakeKeyOp {
		public String make( int num );
	}
	
	public static class MakeKeyOpForMsgKey implements IMakeKeyOp {
		private SimpleDateFormat dateFormat_ = new SimpleDateFormat("yyyyMMddHHmmss");
		private String prefix = "";

		public MakeKeyOpForMsgKey() {
			this.prefix = "MSGKEY_";
		}
		public MakeKeyOpForMsgKey(String prefix) {
			this.prefix = prefix;
		}
		public String make( int num ) {
			return String.format("%s%s_%06d", prefix, dateFormat_.format(new Date()), num );
		}
	}
	
	private static final String LABEL = "HPR MakeKey\r\n";
	private PhysicalFile file_;
	private int currentCount_;
	private IMakeKeyOp makeKeyOp_;
	
	public MakeKey( IMakeKeyOp makeKeyOp ) {
		file_ = new MemoryMapedFile();
		makeKeyOp_ = makeKeyOp;
	}

	public synchronized void open( String path, String fileName ) throws IOException {
		file_.open( path, fileName );

		FileLock lock = file_.getLock();
		try {
			readCurrentCount();
		}
		finally {
			file_.releaseLock( lock );
		}
	}

	public synchronized void create( String path, String fileName ) throws IOException {
		file_.create ( path, fileName );
		
		FileLock lock = file_.getLock();
		try {
			file_.put( 0, LABEL.getBytes(), 0, LABEL.getBytes().length );
			writeCurrentCount();
		}
		finally {
			file_.releaseLock( lock );
		}
	}
	
	public synchronized void createOpen( String path, String fileName ) throws IOException {
		try {
			open( path, fileName );
			return;
		}
		catch( IOException ex ) {}
		
		create( path, fileName );
	}
	
	
	public synchronized String make() throws IOException {

		FileLock lock = file_.getLock();
		try {
			String key = makeKeyOp_.make(++currentCount_);
			
			writeCurrentCount();

			return key;
		}
		finally {
			file_.releaseLock( lock );
		}
	}
	
	
	private void readCurrentCount() throws IOException {
		currentCount_ = file_.getInt( LABEL.getBytes().length );
	}

	private void writeCurrentCount() throws IOException {
		file_.putInt( LABEL.getBytes().length, currentCount_ );
	}

	// public static void main(String[] args) {
	
	// 	MakeKey key = new MakeKey( new MakeKeyOpForMsgKey() );
		
	// 	try {
	// 		key.createOpen("", "make_key.txt");
			
	// 		System.out.println(key.make());
	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
		
		
	// }
}
