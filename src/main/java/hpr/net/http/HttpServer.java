package hpr.net.http;

import hpr.net.INioSelectSingle;
import hpr.net.NioSelectSingle;
import hpr.net.NioServerSingle;
import hpr.util.DumpBytes;
import hpr.util.ExtendByteBuffer;
import hpr.util.KMPMatch;
import hpr.util.StateThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class HttpServer implements INioSelectSingle, IHttpSend {

	private final static int DEFAULT_RECV_BUFF_SIZE	= 1024;
	private final static int MAXIMUM_RECV_BUFF_SIZE	= 64 * 1024 * 1024;		// 64 MBytes
	
	
	private IHttpServer callback_;
	private NioSelectSingle select_;
	private NioServerSingle server_;

	private final int defaultRecvBuffSize_;
	private final int maximumRecvBuffSize_;
	
	
	public HttpServer (IHttpServer callback) throws IOException {
		this( callback, DEFAULT_RECV_BUFF_SIZE, MAXIMUM_RECV_BUFF_SIZE);
	}
	
	public HttpServer (IHttpServer callback, final int defaultRecvBuffSize, final int maxRecvBuffSize) throws IOException {
		
		defaultRecvBuffSize_ = defaultRecvBuffSize;
		maximumRecvBuffSize_ = maxRecvBuffSize;
		
		callback_ = callback;
		
		select_ = new NioSelectSingle(this);
		server_ = new NioServerSingle (select_);
	}
	
	public void bind( int port ) throws IOException {
		server_.bind (port);
	}
	
	public void send( SocketChannel sc, ByteBuffer buffer) {
		select_.send( sc, buffer);
	}

	public boolean work() {
		return select_.work();
	}
	
	public boolean work( int waitInterval) {
		return select_.work( waitInterval );
	}
	
	@Override
	public void onAccept(SelectionKey key) {
		SocketChannel sc = (SocketChannel)key.channel();
		callback_.onAccept(sc);		
	}

	@Override
	public void onConnect(SelectionKey key, IOException ex) {
	}

	@Override
	public void onDisconnected(SelectionKey key) {
		SocketChannel sc = (SocketChannel)key.channel();
		callback_.onDisconnected(sc);		
	}

	@Override
	public void onException(SelectionKey key, IOException ex) {
		SocketChannel sc = (SocketChannel)key.channel();
		callback_.onException(sc, ex);		
	}

	@Override
	public void onException(Exception ex) {
		callback_.onException(ex);		
	}

	@Override
	public void onRead(SelectionKey key) {
		SocketChannel sc = (SocketChannel)key.channel();

		 ByteBuffer buffer = select_.getRecvBuff(sc, defaultRecvBuffSize_);

		 boolean res = read( sc, buffer);
		 
		 if( res ) {
			 buffer.compact();
		 }
		 else {
			buffer.position (buffer.limit());
			buffer.limit(buffer.capacity());

			if( buffer.position() == buffer.capacity()) {
				try {
					buffer = ExtendByteBuffer.extend(buffer, buffer.capacity() * 2, maximumRecvBuffSize_);
					
					select_.putRecvBuff(sc, buffer);
				} catch (Exception ex) {
					callback_.onException(sc, new IOException(ex.getMessage()));	
					
					server_.cancel(sc);
				}
			}
		 }
	}
	
	
	
	private boolean read(SocketChannel sc, ByteBuffer buffer) {

		try {
			int readBytes = select_.read( sc, buffer );
			if( 0 >= readBytes) {
				return true;
			}
//			System.out.println("readBytes: "  + readBytes);
//			System.out.println("new BODY1:\n" + buffer);
//			System.out.println("new BODY2:\n" + DumpBytes.str(buffer.array()));

			int pos = KMPMatch.indexOf (buffer.array(), buffer.position(), buffer.limit(), "\r\n\r\n".getBytes());
			if (-1 == pos) {
				return false;		
			}
		
			HttpRequest req = HttpRequest.parse (buffer);
			if (null == req) {
				return false;		
			}
		
			String contentLen = req.getHeaders().get("Content-Length");
			int len = 0;
			if( null != contentLen) {
				len = Integer.parseInt(contentLen);
				
				if (0 < len) {

					if (pos + 4 + len > buffer.limit()) {
//							System.out.println("모질라! " + (pos + 4 + packet.bodyLen_) + " > " + length);
						return false;		
					}
					
					req.body_ = new byte[len];
					System.arraycopy(buffer.array(), pos+4, req.body_, 0, len);
				}

			
			}
			
			HttpResponse res = new HttpResponse (this, sc);
			
/*				
				boolean isResource = downloadResource( req, res );
				if (!isResource) {
					httpEventImpl_.onHttp( req, res );
				}
*/	
			callback_.onHttp( req, res );

			buffer.position(pos+4+len);
			
			
//			server_.cancel( sc );
			return true;
		}
		catch (Exception ex) {
			callback_.onException( ex );
		}
		return false;		
	
	}

	// public static void main(String[] args) {
	// 	class Http extends StateThread implements IHttpServer {

	// 		private HttpServer httpServer_;
			
	// 		public Http() {
	// 			try {
	// 				httpServer_ = new HttpServer(this);
	// 				httpServer_.bind(4455);

	// 			} catch (IOException e) {
	// 				// TODO Auto-generated catch block
	// 				e.printStackTrace();
	// 			}
	// 		}
			
	// 		@Override
	// 		public void onAccept(SocketChannel sc) {
	// 			// TODO Auto-generated method stub
				
	// 		}

	// 		@Override
	// 		public void onDisconnected(SocketChannel sc) {
	// 			// TODO Auto-generated method stub
				
	// 		}

	// 		@Override
	// 		public void onHttp(HttpRequest req, HttpResponse res) {
	// 			// TODO Auto-generated method stub
				
	// 			if( null != req.getUri() )
	// 				System.out.println(req.getUri().toString() );
				
	// 			res.writeStatusCode (200);
	// 			res.writeContentsType ("text/html");
	// 			try {
	// 				res.writeBody ("TEST:" + req.getUri().toString() );
	// 				res.end();
	// 			} catch (Exception e) {
	// 				// TODO Auto-generated catch block
	// 				e.printStackTrace();
	// 			}
	// 		}

	// 		@Override
	// 		public void onException(SocketChannel sc, IOException ex) {
	// 			// TODO Auto-generated method stub
				
	// 			System.err.println("onExceptionA:" + ex);
	// 		}

	// 		@Override
	// 		public void onException(Exception ex) {
	// 			// TODO Auto-generated method stub
	// 			System.err.println("onException:" + ex);

	// 		}

	// 		@Override
	// 		public void onStart() {
	// 			// TODO Auto-generated method stub
	// 			System.err.println("onStart");
	// 		}

	// 		@Override
	// 		public void onStop() {
	// 			// TODO Auto-generated method stub
	// 			System.err.println("onStop");
	// 		}

	// 		@Override
	// 		protected boolean onRun() {
				
	// 			httpServer_.work();
	// 			return true;
	// 		}
			
	// 	}
		
	// 	Http http = new Http();
		
	// 	http.start();
	// }

}
