package hpr.net;

import hpr.net.http.HttpRequest;
import hpr.util.DumpBytes;
import hpr.util.KMPMatch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.Iterator;

public class NioBlockClient {

	private	SocketChannel sc_ = null;
	private	Selector selector_ = null;

	private ByteBuffer recvBuffer_ = ByteBuffer.allocate(1);

	private final int readTimeout_;
	
	public NioBlockClient() throws IOException {
		this(5000);
	}
	public NioBlockClient(int readTimeout) throws IOException {
		readTimeout_ = readTimeout;
	}
	
	public void connect( String host, int port ) throws IOException {
		connect( host, port, 5000 );
	}
	
	public void connect( String host, int port, int timeout ) throws IOException {
	
		disconnect();

		selector_ = SelectorProvider.provider().openSelector();

		sc_ = SocketChannel.open();  
		sc_.configureBlocking(false);  
		sc_.connect(new InetSocketAddress(host, port));  
		sc_.register(selector_, SelectionKey.OP_READ);  
		
		while( !sc_.finishConnect() ) {
			if(timeout > 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				timeout -= 100;
			}
			else {
				throw new IOException("Failed to connect (" + host + ":" + port );
			}
		}
		
	}
	
	public void disconnect() {
		if( null != sc_ ) {
			SelectionKey key = sc_.keyFor(selector_);
			if( null != key ) {
				key.cancel();
			}
			try {
				sc_.socket().close();
			} catch (IOException e) {}
			try {
				sc_.close();
			} catch (IOException e) {}
		}
		sc_ = null;
		
		if( null != selector_ ) {
			try {
				selector_.close();
			} catch (IOException ex) {}
		}
		selector_ = null;
		
		recvBuffer_.clear();
		Arrays.fill( recvBuffer_.array(), 0, recvBuffer_.array().length, (byte)0);
	}


	public void write( byte[] array, int offset, int length ) throws IOException {
		write( ByteBuffer.wrap(array, offset, length));
	}

	
	public void write( byte[] array ) throws IOException {
		write( ByteBuffer.wrap(array));
	}
	
	public void write( ByteBuffer buff ) throws IOException {
		try {
			sc_.write( buff );
		}
		catch( IOException ex ) {
			disconnect();
			throw ex;
		}
	}
	
	public byte[] read( final int length ) throws IOException {
		
		if( recvBuffer_.capacity() < length ) {
			ByteBuffer temp = ByteBuffer.allocate(length);
			
			temp.put(recvBuffer_.array(), 0, recvBuffer_.position());
			recvBuffer_ = temp;
		}
		
		recvBuffer_.limit(length);

		while (true) {
			
			int recvLen = this.read( recvBuffer_ );

			if( 0 == recvLen )
				break;
			
			if( recvBuffer_.position() == length) {
				break;
			}
		}
		
		byte[] data = new byte[recvBuffer_.position()];
		System.arraycopy(recvBuffer_.array(), 0, data, 0, data.length);

		recvBuffer_.position(length);
		recvBuffer_.compact();
	
		return data;
	}
	
	public byte[] readLine() throws IOException {
		final String NEW_LINE = "\r\n";
		
		return readIf( NEW_LINE.getBytes() );
	}
	
	public byte[] readIf( final byte[] delimeter) throws IOException {

		int pos = -1;
		while (true) {
			
//			System.out.println("step1:" + recvBuffer_);
			
			pos = KMPMatch.indexOf (recvBuffer_.array(), 0, recvBuffer_.position(), delimeter);
//			System.out.println("new line?:" + pos);

			if (-1 != pos) {
				break;
			}

			if(!recvBuffer_.hasRemaining()) {
				int capacity = recvBuffer_.capacity();
				ByteBuffer largeBuffer = ByteBuffer.allocate(capacity * 2); // Must have max length
				largeBuffer.put(recvBuffer_.array(), 0, recvBuffer_.position());
				largeBuffer.position(capacity);
				largeBuffer.limit(largeBuffer.capacity());
				
				recvBuffer_ = largeBuffer;
				
//				System.out.println("step2:" + recvBuffer_);

			}
			
			int recvLen = this.read( recvBuffer_ );

	//		System.out.println("step3:" + recvBuffer_);
	//		System.out.println(DumpBytes.str(recvBuffer_.array()));

			if( 0 == recvLen ) {
				break;
			}

		}
		
		if( -1 == pos ) {
			return new byte[0];
		}
		
		pos += delimeter.length;
		byte[] data = new byte[pos];
		System.arraycopy(recvBuffer_.array(), 0, data, 0, data.length);

		int newPos = recvBuffer_.position() - pos;
		recvBuffer_.position(pos);
//		System.out.println("4-1:" + recvBuffer_);
		recvBuffer_.compact();
		recvBuffer_.position(newPos);
//		System.out.println("4-2:" + DumpBytes.str(recvBuffer_.array()));
		
		return data;
	}
	
	private int read( ByteBuffer buffer ) throws IOException {
	
		Iterator<SelectionKey> iter = null; 
		
		int recvLen = 0;
		
		int sleepInterval = 100;
		for( int i = 0; i * sleepInterval < readTimeout_; ++i )  {
			selector_.select(sleepInterval);
			
			iter = selector_.selectedKeys().iterator();  
			if( iter.hasNext() ) {
				break;
			}
		}		
		
		while( null != iter && iter.hasNext()) {  
			SelectionKey key = iter.next();  
			if (key.isReadable()) {  
				int len = sc_.read(buffer);
				
				if (len < 0) {
					key.cancel();
					disconnect();
					throw new IOException("connection was disconnected");
				} else if (len == 0) {  
					continue;  
				}
				recvLen += len;
			}
		}
		return recvLen;
	}

	
	
	private static void test1() {
		try {
			NioBlockClient c = new NioBlockClient();
			System.out.println("connecting");
			c.connect("127.0.0.1", 4040, 3000);
//			boolean ret = c.connect("173.194.127.129", 80, 3000);
			System.out.println("connected");


			c.write("GET / HTTP/1.0\r\n\r\n".getBytes());
/*			
			for( int i = 1; i < 10; ++i) {
				byte[] data = c.read(7);
				if (data.length != 7) {
					System.out.println("not enough:" + new String(data));
					break;
				}
				System.out.println("read: " + new String(data));
			}
*/
			
			while(true) {
				byte[] data = c.readLine();

				if (data.length == 0) {
					System.out.println("not enough:" + new String(data));
					break;
				}
				System.out.println("readLine: " + new String(data));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void test2() {
		try {
			NioBlockClient c = new NioBlockClient();
			System.out.println("connecting");

			c.connect("173.194.127.129", 80, 3000);
			System.out.println("connected");


			c.write("GET / HTTP/1.0\r\n\r\n".getBytes());

			while(true) {
				byte[] data = c.readLine();

				if (data.length == 0) {
					System.out.println("not enough:" + new String(data));
					break;
				}
				System.out.println("readLine: " + new String(data));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void test3() {
		try {
			NioBlockClient c = new NioBlockClient();
			System.out.println("connecting");

			//c.connect("www.google.com", 80, 3000);
			c.connect("173.194.127.129", 80, 3000);
			System.out.println("connected");


			c.write("GET / HTTP/1.0\r\n\r\n".getBytes());

			byte[] data = c.readIf("\r\n\r\n".getBytes());
			if( null == data ) {
				System.out.println("nothing");
				return;
			}
			System.out.println("" + DumpBytes.str(data));

			
			
			HttpRequest req = HttpRequest.parse(ByteBuffer.wrap(data));
			if( null == req ) {
				System.out.println("failed to paring");
				return;
			}
			
			String contentLen = req.getHeaders().get("Content-Length");
			if( null != contentLen) {
				int len = Integer.parseInt(contentLen);
				
				System.out.println("Len:" + len);
				
				byte[] body = c.read(len);
				
				if (body.length == 0) {
					System.out.println("not enough:" + new String(body));
				}
				else
					System.out.println("Body: [" + new String(body) +"]");
				
				System.out.println("" + DumpBytes.str(body));
}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	// public static void main(String[] args) {
		
	// 	test3();
	// }
}
