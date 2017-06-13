package hpr.net;

import hpr.util.StateThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class NioServerSingle {

	private ServerSocketChannel server_ = null;
	
	private NioSelectSingle nioSelect_;

	public NioServerSingle( NioSelectSingle nioSelect ) {
		nioSelect_ = nioSelect;
	}
	
	public void bind (int port) throws IOException {
		server_ = ServerSocketChannel.open();

		server_.socket().bind(new InetSocketAddress(port));
		server_.configureBlocking(false);

		nioSelect_.regist ( server_ );
	}
	
	public void cancel( SocketChannel sc) {
		nioSelect_.cancel(sc);
	}
	

// 	public static void main(String[] args) {
		
// 		class Server extends StateThread implements INioSelectSingle {
// 			private NioSelectSingle select_;
// 			private NioServerSingle server_;
					
// 			public Server() throws IOException {
// 				select_ = new NioSelectSingle(this);
// 				server_ = new NioServerSingle(select_);
				
// 				server_.bind(4040);
// 				this.start();
// 			}

// 			@Override
// 			public void onConnect(SelectionKey key, IOException ex) {
			
// 			}

// 			@Override
// 			public void onAccept(SelectionKey key) {
// 				System.out.println("onAccept:" + key);				
// 			}
			
// 			@Override
// 			public void onDisconnected(SelectionKey key) {
// 				System.out.println("onDisconnected:" + key);				
// 			}

// 			@Override
// 			public void onRead(SelectionKey key) {
				
// 				onRead( key, ByteBuffer.allocate(1024) );
// 			}
			
// 			public boolean onRead(SelectionKey key, ByteBuffer buffer) {
// 				byte[] data = new byte[buffer.limit()];
// 				System.arraycopy(buffer.array(), 0, data, 0, data.length);
				
// 				System.out.println("read: " + new String(data));

// 				SocketChannel sc = (SocketChannel)key.channel();
// 				try {
// /*					
// 					for(int i = 0; i < 20; ++i) {
// 						sc.write (ByteBuffer.wrap(("" + i).getBytes()));
// 						Thread.sleep(1000);
// 					}
// */
					
// 					for(int i = 1; i <= 20; ++i) {
// 						StringBuffer sb = new StringBuffer();
// 						for( int j = 0; j < i; ++j) {
// 							sb.append(i);
// 						}
// 						sb.append("\r\n");
						
// 						for( int j = 0; j < i; ++j) {
// 							sb.append("a"+i);
// 						}
// 						sb.append("\r\n");
						
// 						int res = sc.write (ByteBuffer.wrap(sb.toString().getBytes()));
// 						System.out.println("sent:" + res);
// 						Thread.sleep(1000);
// 					}

					
// 				} catch (IOException e) {
// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 					return false;
// 				} catch (InterruptedException e) {
// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 					return false;
// 				}
// 				return true;
// 			}

// 			@Override
// 			public void onException(SelectionKey key, IOException ex) {
// 				System.out.println("onException:" + ex);				
				
// 			}

// 			@Override
// 			public void onException(Exception ex) {
// 				System.out.println("onException:" + ex);				
// 			}

// 			@Override
// 			public void onStart() {
// 				// TODO Auto-generated method stub
				
// 			}

// 			@Override
// 			public void onStop() {
// 				// TODO Auto-generated method stub
				
// 			}

// 			@Override
// 			protected boolean onRun() {

// 				select_.work();
// 				return true;
// 			}
			

			
// 		}
				
// 		try {
// 			Server server = new Server();
// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
// 	}
}
