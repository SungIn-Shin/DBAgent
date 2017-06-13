package hpr.net;

import hpr.util.ExtendByteBuffer;
import hpr.util.StateThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioClient extends NioSession {
	
	private NioSelect nioSelect_;
	private INioClient callback_;

	public NioClient( NioSelect nioSelect, INioClient callback ) {
		
		super(nioSelect);
		
		if( null == nioSelect )
			throw new RuntimeException( "nioSelect should not be NULL");
		nioSelect_ = nioSelect;
		
		if( null == callback )
			throw new RuntimeException( "callback should not be NULL");
		callback_ = callback;
		
	}
	
	public void connect( String ip, int port ) throws IOException {
		socketChannel_ = SocketChannel.open();
		socketChannel_.configureBlocking(false);
		socketChannel_.connect(new InetSocketAddress(ip, port));

		nioSelect_.regist( socketChannel_, callback_);
	}
	
	public boolean hasSendData() {
		if( null == socketChannel_ )
			return false;

		return nioSelect_.hasSendData( socketChannel_ );
	}
	
	public boolean isConnected () {
		if( null == socketChannel_ )
			return false;
		
		return socketChannel_.isConnected();
	}
	
// 	public static void main(String[] args) {
		
// 		class Client extends StateThread implements INioSelect, INioClient {

// 			public NioSelect select_;
// 			public NioClient client_;
			
// 			public Client() throws IOException {
// 				select_ = new NioSelect(this);
// 				client_ = new NioClient( select_, this );
// 			}

// 			@Override
// 			public void onStart() {
// 				System.out.println("thread start");
// 			}

// 			@Override
// 			public void onStop() {
// 				System.out.println("thread stop");
// 			}

// 			@Override
// 			public INioSession onAccept(NioSession session) {
// 				// TODO Auto-generated method stub
// 				return null;
// 			}

// 			@Override
// 			public void onException(Exception ex) {
// 				System.out.println( "EXCEPTION:" + ex.getMessage());
// 			}

			
// 			@Override
// 			public void onDisconnected() {
// 				System.out.println( "connection is disconnected ");
// 			}
// /*
// 			@Override
// 			public boolean onRead(ByteBuffer buffer) {
// 				byte[] data = new byte[buffer.limit()];
// 				System.arraycopy(buffer.array(), 0, data, 0, data.length);
				
// 				System.out.println("read: " + new String(data));

// 				return false;			
// 			}
// */
// 			@Override
// 			public void onException(IOException ex) {
// 				System.out.println( "IOException:" + ex.getMessage());
// 			}

// 			@Override
// 			public void onConnect(IOException ex) {
// 				if( ex == null ) {
// 					System.out.println( "connection is success");
					
// 					client_.send ("GET / HTTP/1.0\r\n\r\n".getBytes());
// 				}
// 				else {
// 					System.out.println( "Failed to connect! : " + ex.getMessage());
// 				}				
// 			}

// 			@Override
// 			public void onRead(SelectionKey key) {
// 				// TODO Auto-generated method stub
// 				SocketChannel sc = (SocketChannel)key.channel();

// 				ByteBuffer buffer = select_.getRecvBuff(sc, 1024);
// 				try {

// 					int readBytes = select_.read( sc, buffer );

// 					if( 0 < readBytes) {
// 						System.out.println("read: " + new String(buffer.array()));

// 						buffer.compact();
// 					}
// 					else {
// 						buffer.position (buffer.limit());
// 						buffer.limit(buffer.capacity());

// 						if( buffer.position() == buffer.capacity()) {
// 							try {
// 								buffer = ExtendByteBuffer.extend(buffer, buffer.capacity() * 2, 1024 * 1024 * 4);

// 								select_.putRecvBuff(sc, buffer);
// 							} catch (Exception ex) {
// 								ex.printStackTrace();

// 								client_.disconnect();
// 							}
// 						}
// 					}
// 				} catch (IOException e) {
// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 				}
// 			}

// 			@Override
// 			protected boolean onRun() {
				
// 				select_.work();
				
// 				return true;
// 			}
// 		}
		
// 		try {
// 			Client c = new Client();

// 			c.start();
// 			c.client_.connect("216.58.221.68", 80);
// 			Thread.sleep(3000);
// 			c.client_.disconnect();
// 			System.out.println("next!");

// 			c.client_.connect("216.58.221.68", 80);
// 			Thread.sleep(3000);
// 			c.client_.disconnect();
// 			c.stop();

// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		} catch (InterruptedException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
		
// 	}
	
		
}
