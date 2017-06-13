package hpr.net.http;


import hpr.util.ExtendByteBuffer;
import hpr.util.KMPMatch;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;



public class HttpResponse {
	
	private static final int MAX_BUFFER_SIZE = 1024 * 1024 * 64;		// 64 MBytes


	private ByteBuffer bufferHead_ = ByteBuffer.allocate(1024);
	private ByteBuffer bufferBody_ = ByteBuffer.allocate(1024);
	
	private IHttpSend server_;
	private SocketChannel sc_;
	
	private String version_;
	private int status_;
	
	private Map<String, String> headers_;
	
	public HttpResponse() {
	}

	public HttpResponse( IHttpSend server, SocketChannel sc ) {
		server_ = server;
		sc_ = sc;
	}
	
	public final String getVersion() {
		return version_;
	}
	
	public int getStatusCode() {
		return status_;
	}
	
	public Map<String, String> getHeaders() {
		return headers_;
	}
	
	public void writeStatusCode( int status ) {
		
		bufferHead_.clear();
		bufferBody_.clear();
		
		status_ = status;

		switch (status) {
		case 200:
			bufferHead_.put( "HTTP/1.1 200 OK\r\n".getBytes() );	
			break;
		case 201:
			bufferHead_.put( "HTTP/1.1 201 Created\r\n".getBytes() );	
			break;
		case 202:
			bufferHead_.put( "HTTP/1.1 202 Accepted\r\n".getBytes() );	
			break;
		case 203:
			bufferHead_.put( "HTTP/1.1 203 Non-Authoritative Information\r\n".getBytes() );	
			break;
		case 204:
			bufferHead_.put( "HTTP/1.1 204 No Content\r\n".getBytes() );	
			break;
		case 205:
			bufferHead_.put( "HTTP/1.1 205 Reset Content\r\n".getBytes() );	
			break;

		case 301:
			bufferHead_.put( "HTTP/1.1 301 Moved Permanently\r\n".getBytes() );	
			break;

		case 400:
			bufferHead_.put( "HTTP/1.1 400 Bad Request\r\n".getBytes() );
			break;
		case 401:
			bufferHead_.put( "HTTP/1.1 401 Unauthorized\r\n".getBytes() );
			break;
		case 403:
			bufferHead_.put( "HTTP/1.1 403 Forbidden\r\n".getBytes() );
			break;
		case 404:
			bufferHead_.put( "HTTP/1.1 404 Not found\r\n".getBytes() );
			break;
			
		default:
			bufferHead_.put( "HTTP/1.1 500 Internal Server Error\r\n".getBytes() );
		}
	}
	
	public void writeContentsType( String contentsType) {
		bufferHead_.put( "Content-Type: ".getBytes() );
		bufferHead_.put( contentsType.getBytes() );
		bufferHead_.put( "\r\n".getBytes() );
	}

	public void writeSetCookie( String name, String value ) {
		bufferHead_.put( "Set-Cookie: ".getBytes() );
		bufferHead_.put( name.getBytes() );
		bufferHead_.put( "=".getBytes() );
		bufferHead_.put( value.getBytes() );
		bufferHead_.put( ";".getBytes() );
		bufferHead_.put( "\r\n".getBytes() );
	}
	
	public void writeBody (String body) throws Exception {
		bufferBody_ = ExtendByteBuffer.extend(bufferBody_, body.getBytes().length, MAX_BUFFER_SIZE);
		
		bufferBody_.put(body.getBytes());
	}
	
	public void writeBody (ByteBuffer body) throws Exception {
		bufferBody_ = ExtendByteBuffer.extend(bufferBody_, body.limit(), MAX_BUFFER_SIZE);

		bufferBody_.put(body);
	}
	
	public void writeBody (byte[] body) throws Exception {
		bufferBody_ = ExtendByteBuffer.extend(bufferBody_, body.length, MAX_BUFFER_SIZE);

		bufferBody_.put(body);
	}
	
	public void end () throws Exception {

		
		bufferHead_.put( "Connection: close\r\n".getBytes() );
		bufferHead_.put( "Cache-Control: max-age=0, no-cache, no-store\r\n".getBytes() );
		bufferHead_.put( "Content-Length: ".getBytes() );
		bufferHead_.put( ("" + bufferBody_.position()).getBytes() );
		bufferHead_.put( "\r\n".getBytes() );
		bufferHead_.put( "\r\n".getBytes() );
		
		ByteBuffer buffer = ExtendByteBuffer.extend(bufferHead_, bufferBody_.position(), MAX_BUFFER_SIZE);
		buffer.put(bufferBody_.array(), 0, bufferBody_.position());

		buffer.flip();
		server_.send( sc_, buffer);

		
		
	//	bufferHead_.flip();
	//	bufferHead_.compact();
	//	server_.send( sc_, bufferHead_.array(), bufferHead_.position());
	
	//	bufferBody_.flip();
	//	bufferBody_.compact();
	//	server_.send( sc_, bufferBody_.array(), bufferBody_.position());
		
	 
	/*	
		String body = "aaa";
	
		String str = "HTTP/1.1 200 OK\r\n" 
				//	+ "Date: Sun, 10 Oct 2010 23:26:07 GMT\r\n"
			//		+ "Server: Hopper/1.1.0\r\n"
			//		+ "Last-Modified: Sun, 26 Sep 2010 22:04:35 GMT\r\n"
				//	+ "ETag: \"45b6-834-49130cc1182c0\"\r\n"
			//		+ "Accept-Ranges: bytes\r\n"
					+ "Content-Length: {0}\r\n"
					+ "Connection: close\r\n"
					+ "Content-Type: text/html\r\n"
					+ "\r\n"
					+ "{1}";
			
			
			byte[] sendMsg = MessageFormat.format(str, body.length(), body).getBytes();
			
			ByteBuffer buffer = ByteBuffer.allocate(sendMsg.length + 1);
			
			buffer.put(sendMsg);
		//	buffer.put(new byte[]{0});
			buffer.flip();

			server_.write(sessionKey_, buffer);
	*/
	}
	

	static public HttpResponse parse (ByteBuffer buffer) {
		
		
		int pos = KMPMatch.indexOf (buffer.array(), buffer.position(), buffer.limit(), "\r\n\r\n".getBytes());
		if (-1 == pos) {
			return null;
		}
		
		HttpResponse packet = new HttpResponse();
		packet.headers_ = new HashMap<String, String>();
		
		int bodyLen = 0;
		
		byte[] buff = buffer.array();
		
		int beginPos = buffer.position();
		int length = buffer.limit();
		
	//	System.out.println("beginPos:" + beginPos + "," + length);
		//System.out.println("RAW:" + new String(buff, beginPos, pos-beginPos));	

		boolean isFirst = true;
		while (true) {
			
		//	System.out.println("beginPos:" + beginPos + " length-beginPos:" + (length-beginPos));
			int endPos = KMPMatch.indexOf (buff, beginPos, length-beginPos, "\r\n".getBytes());
			if (-1 == endPos) {
				break;
			}

			//System.out.println("endPos:" + endPos);
			
			//System.out.println("beginPos:" + beginPos + " endPos-beginPos:" + (endPos-beginPos));
				
			int middlePos = KMPMatch.indexOf (buff, beginPos, endPos-beginPos, (isFirst ? " " : ":").getBytes());
			if (-1 == middlePos) {
				break;
			}
			//System.out.println("endPos:" + endPos + " middlePos:" + middlePos);
			
			String key 	= new String (buff, beginPos, middlePos-beginPos).trim();
			String value= new String (buff, middlePos+1, endPos-middlePos).trim();
			
//			System.out.println("[" + key + "] [" + value + "]" );

			if (isFirst) {
				packet.version_ = new String (key);
				
				int urlPos = KMPMatch.indexOf (value.getBytes(), 0, value.getBytes().length, " ".getBytes());
				if (-1 != urlPos) {
					String url = new String (value.substring(0, urlPos));
						
					packet.status_ = Integer.parseInt(url);
				}
				isFirst = false;
			}
			else {
				packet.headers_.put(key, value);
			}
			
			
			if (0 == key.compareTo("Content-Length")) {
				bodyLen = Integer.parseInt(value);
			}
			
			beginPos = endPos + 2;
		}
/*
		if (0 < bodyLen) {

			if (pos + 4 + bodyLen > length) {
//				System.out.println("모질라! " + (pos + 4 + packet.bodyLen_) + " > " + length);
				return null;
			}
			
			packet.body_ = new byte[bodyLen];
			System.arraycopy(buff, pos+4, packet.body_, 0, bodyLen);
	//		System.out.println("BODY:" + packet.body_);
		}
*/		
		
		return packet;
	}
}

