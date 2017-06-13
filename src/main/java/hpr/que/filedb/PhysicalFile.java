package hpr.que.filedb;

import hpr.util.Files;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public abstract class PhysicalFile {
	
	private String pathName_;
	private String fileName_;
	
	private File file_;
	protected RandomAccessFile randomAccessFile_;
	
	protected FileChannel fileChannel_;

	public void create (String fileName) throws IOException {
		create ("", fileName);
	}

	public void create (String pathName, String fileName) throws IOException {
		fileOpen( pathName, fileName, false );
	}

	public void open (String fileName) throws IOException {
		open ("", fileName);
	}
	
	public void open (String pathName, String fileName) throws IOException {
		fileOpen( pathName, fileName, true );
	}

	public void createOpen () throws IOException {
		createOpen (this.pathName_, this.fileName_);
	}

	public void createOpen (String fileName) throws IOException {
		try {
			open(fileName);
			return;
		}
		catch (IOException ex) {}

		create(fileName);
	}

	public void createOpen (String pathName, String fileName) throws IOException {
		try {
			open(pathName, fileName);
			return;
		}
		catch (IOException ex) {}

		create(pathName, fileName);
	}

	public void close () throws IOException {

		if( null != randomAccessFile_ ) {
			randomAccessFile_.close();
		}
		randomAccessFile_ = null;
		
		file_ = null;
	}
	
	public boolean exists() {
		
		if (null == file_)
			return false;
		
		return file_.exists();
	}
	
	// caller should call releaseLock() later 
	public FileLock getLock() throws IOException {
		return fileChannel_.lock();
	}
	
	public void releaseLock( FileLock lock ) {
		if( null == lock || !lock.isValid())
			return;
		
		try {
			lock.release();
		}
		catch( IOException ex ) {}
	}
	
	protected void fileOpen (String pathName, String fileName, boolean isOpen) throws IOException {

		if( null == fileName || fileName.equals("")) {
			throw new IOException ("File name should not be null or empty string");
		}

		String extraPath = Files.getPath(fileName);
		if( !extraPath.isEmpty() ) {
			
			if( null == pathName || pathName.equals("")) {
				pathName = extraPath;
			}
			else {
				pathName = pathName + System.getProperty("file.separator") + extraPath;
			}
			fileName = Files.getFileName(fileName);
		}
		
		if( null == pathName || pathName.equals("")) {
			file_ = new File( fileName );
		}
		else {
			file_ = new File( pathName, fileName );
		}
		
		if( isOpen ) {
			if (!file_.exists())
				throw new IOException ("File not found: " + pathName + fileName);
		}
		else {
			
			if( null != pathName && !pathName.equals("")) {
				File dir = new File( pathName );
				if( !dir.isDirectory()) {
					dir.mkdirs();
				}
			}
			if (!file_.createNewFile())
				throw new IOException ("File already exist: " + pathName + fileName);
		}
		randomAccessFile_ = new RandomAccessFile( file_, "rw" );
		
		fileChannel_ = randomAccessFile_.getChannel();
		
		pathName_ = pathName;
		fileName_ = fileName;
	}

	abstract public void put( long position, byte[] src, int offset, int length ) throws IOException;
	abstract public void get( long position, byte[] dst, int length ) throws IOException;

	abstract public int getInt( long position ) throws IOException;
	abstract public void putInt( long position, int value ) throws IOException;


	
}
