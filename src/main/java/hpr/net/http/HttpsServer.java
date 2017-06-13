package hpr.net.http;

import hpr.net.INioSelectSingle;
import hpr.net.NioSelectSingle;
import hpr.net.NioServerSingle;
import hpr.net.ssl.SSLServer;
import hpr.util.DumpBytes;
import hpr.util.ExtendByteBuffer;
import hpr.util.KMPMatch;
import hpr.util.StateThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;



public class HttpsServer implements INioSelectSingle, IHttpSend {
	
	private final static int DEFAULT_RECV_BUFF_SIZE	= 1024;
	private final static int MAXIMUM_RECV_BUFF_SIZE	= 64 * 1024 * 1024;		// 64 MBytes

	private IHttpServer callback_;
	private NioSelectSingle select_;
	private NioServerSingle server_;
	
	private String keyStorePath_;
	private String storePwd_;

	private final int defaultRecvBuffSize_;
	private final int maximumRecvBuffSize_;

	private ConcurrentHashMap<SocketChannel, SSLServer> sslServers_ = null;

	public HttpsServer (IHttpServer callback) throws IOException {
		this( callback, DEFAULT_RECV_BUFF_SIZE, MAXIMUM_RECV_BUFF_SIZE);
	}

	public HttpsServer (IHttpServer callback, final int defaultRecvBuffSize, final int maxRecvBuffSize) throws IOException {

		defaultRecvBuffSize_ = defaultRecvBuffSize;
		maximumRecvBuffSize_ = maxRecvBuffSize;


		callback_ = callback;

		select_ = new NioSelectSingle(this);
		server_ = new NioServerSingle (select_);

		sslServers_ = new ConcurrentHashMap<SocketChannel, SSLServer>();
	}
	
	public void bind( int port ) throws IOException {
		server_.bind (port);
	}
	
	public boolean work() {
		return select_.work();
	}
	
	public boolean work( int waitInterval) {
		return select_.work( waitInterval );
	}
	
	public void keyStore( final String keyStorePath, final String storePwd ) {
		
		keyStorePath_ = keyStorePath;
		storePwd_ = storePwd;
	}
	
	@Override
	public void send( SocketChannel sc, ByteBuffer buffer) {
		
		SSLServer sslServer = sslServers_.get(sc);

		try {
			while( buffer.hasRemaining() ) {
				
				ByteBuffer encBuff = sslServer.encrypt(buffer);
				buffer.compact();
				buffer.flip();
			
				ByteBuffer enc = ByteBuffer.allocate(encBuff.limit()); 
				enc.put(encBuff);
				enc.flip();
				select_.send(sc, enc);
				
			}
		} catch (SSLException ex) {
			callback_.onException(ex);	
		}
	}

	
	private void shutdown(SocketChannel sc)
	{
		try
		{
			SSLServer sslServer = sslServers_.get(sc);
			if( null != sslServer) {
				sslServers_.remove(sc);
			
				sslServer.closeOutbound();
				sslServer.closeInbound();
			}
		}
		catch(Exception e)
		{
		}
		finally {
//			server_.cancel(sc);
		}
	}
	
	
	
	@Override
	public void onAccept(SelectionKey key) {
		SocketChannel sc = (SocketChannel)key.channel();

		try {
			SSLServer sslServer = new SSLServer(keyStorePath_, storePwd_.toCharArray(), sc);
			sslServer.beginHandShake();		
			
			sslServers_.put(sc, sslServer);
		} catch (Exception ex) {
			try {
				sc.close();
			} catch (IOException e) {}

			callback_.onException(ex);	
		}
		
		callback_.onAccept(sc);		
	}

	@Override
	public void onConnect(SelectionKey key, IOException ex) {
	}

	@Override
	public void onDisconnected(SelectionKey key) {
		
		SocketChannel sc = (SocketChannel)key.channel();
		try {
			callback_.onDisconnected(sc);
		}
		finally {
			shutdown(sc);
		}
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
	public void onRead(SelectionKey key ) {
		
		SocketChannel sc = (SocketChannel)key.channel();
		
		SSLServer sslServer = sslServers_.get(sc);


		if(sslServer.getHandShakeStatus() != HandshakeStatus.NOT_HANDSHAKING
				&& sslServer.getHandShakeStatus() != HandshakeStatus.FINISHED)
		{
			try {
				sslServer.handshake(sc);
			} catch (IOException ex) {
				callback_.onException( sc, ex );
			}
			return ;
		}
		else
		{
			try {
				ByteBuffer encryptBuffer = sslServer.getRecvEncryptedBuffer();
				int readBytes = select_.read( sc, encryptBuffer );

				if( 0 >= readBytes) {
					return;
				}

				ByteBuffer decryptBuffer = sslServer.decryptForRead( encryptBuffer );

				ByteBuffer buffer = select_.getRecvBuff(sc, defaultRecvBuffSize_);

				buffer = ExtendByteBuffer.extend(buffer, decryptBuffer.limit(), maximumRecvBuffSize_);
				buffer.put(decryptBuffer);

				buffer.flip();

				boolean res = this.read(sc, buffer);

				if( res ) {
					buffer.compact();
				}
				else {
					buffer.position (buffer.limit());
					buffer.limit(buffer.capacity());
				}
				
			} catch (Exception ex) {
				callback_.onException(sc, new IOException(ex.getMessage()));	

				server_.cancel(sc);
			}
		}
	}




	private boolean read(SocketChannel sc, ByteBuffer buffer) throws URISyntaxException {
	
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
		
		callback_.onHttp( req, res );
		
		buffer.position(pos+4+len);


		//			server_.cancel( sc );
		return true;
	}

// 	public static void main(String[] args) {
// 		class Http extends StateThread implements IHttpServer {

// 			private HttpsServer httpsServer_;
			
// 			public Http() {
// 				try {
					
// 					httpsServer_ = new HttpsServer(this);
// 					httpsServer_ .keyStore("D:/Project/2013/유사도/JWS/Hopper/src/hpr/net/ssl/keystore/SimpleNIOServer.keystore", "example");

// 					httpsServer_.bind(4456);

// 				} catch (IOException e) {
// 					System.out.println("Http");
// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 				}
// 			}
			
// 			@Override
// 			public void onAccept(SocketChannel sc) {
// 				// TODO Auto-generated method stub
// 				System.out.println("onAccept");

// 			}

// 			@Override
// 			public void onDisconnected(SocketChannel sc) {
// 				// TODO Auto-generated method stub
// 				System.out.println("onDisconnected");
				
// 			}

// 			@Override
// 			public void onHttp(HttpRequest req, HttpResponse res) {
// 				// TODO Auto-generated method stub
// 				System.out.println("--onHttp");
// /*
// 				if( null != req.getUri() )
// 					System.out.println("onHttp:" + req.getUri().toString() );
				
// 				res.writeStatusCode (200);
// 				res.writeContentsType ("text/html");
// 				try {
// 					res.writeBody ("TEST~11~sssssszzzs:" + req.getUri());
// 					res.end();

// 				} catch (Exception e) {
// 					System.out.println("onHttp:");

// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 				}
// */
// 				String uriPath = req.getUri().getPath();
		
// 				try {
					
// 					String fileName = new String("d:/");
// 					try {
// 						if( null != uriPath && !uriPath.isEmpty() && !uriPath.equals("/") ) {
// 							fileName += uriPath;
// 						}
						
// 						writeContentFile( fileName, res);
// 					}
// 					catch( IOException ex) {
	
// 						res.writeStatusCode (404);
// 						res.writeContentsType ("text/html");
// 						res.writeBody ("<!DOCTYPE html>"
// 										+ "<html lang='en'>"
// 										+ "<head>"
// 										+ "<title>404 Page Not Found</title>"
// 										+ "</head>"
// 										+ "<body>"
// 										+ "	<div id=\"container\">"
// 										+ "		<h1>404 Page Not Found</h1>"
// 										+ "		<p>The page you requested was not found.</p></div>"
// 										+ "</body>"
// 										+ "</html>");
// 						res.end();
	
// 					}
// 				}
// 				catch(Exception ex) {
// 					ex.printStackTrace();
// 				}
				
// 			}
			
			
// 			private void writeContentFile( String fileName, HttpResponse res ) throws Exception {
				
// 				File file = new File(fileName);
// 				FileInputStream fs = null;
				
// 				try {
// 					fs = new FileInputStream(file);
			
// 					String fileExtName = hpr.util.Files.getFileNameExtention(fileName);
// 					String contentType = hpr.net.http.InternetMediaType.map( fileExtName );
					
// 					res.writeStatusCode (200);
// 					res.writeContentsType (contentType);
					
// 					ByteBuffer buffer = ByteBuffer.allocate(1024);
// 					while( -1 != fs.getChannel().read(buffer) ) {
// 						buffer.flip();
// 						res.writeBody (buffer);
// 						buffer.clear();
// 					}
// 					res.end();
// 				}
// 				finally {
// 					if( null != fs)
// 						fs.close();
// 				}
// 			}
			

// 			@Override
// 			public void onException(SocketChannel sc, IOException ex) {
// 				// TODO Auto-generated method stub
// 				System.out.println("onException:" + ex);

// 			}

// 			@Override
// 			public void onException(Exception ex) {
// 				// TODO Auto-generated method stub
// 				ex.printStackTrace();
// 				System.out.println("onExceptionA:" + ex);

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
// 				httpsServer_.work();
// 				return true;
// 			}
			
// 		}
		
// 		Http http = new Http();
// 		http.start();
// 	}

}
