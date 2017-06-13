package hpr.net.http;

import hpr.net.NioBlockClient;
import hpr.util.DumpBytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HttpClient {
	
	private List<String> cookies_ = new ArrayList<String>();
	
	
	private HttpResponse response_;
	private byte[] body_;
	
	String method_;
	String host_;
	int port_;
	
	StringBuilder packet_;
	
	public HttpClient() throws IOException {
	}
	
	public void get( String host, int port, String url ) throws IOException {
		ready( "GET", host, port, url );
	}	

	public void post( String host, int port, String url ) throws IOException {
		ready( "POST", host, port, url );
	}	

	public void ready( String method, String host, int port, String url ) throws IOException {
		
		method_ = method;
		host_ = host;
		port_ = port;
		
		response_ = null;
		body_ = null;
		
		packet_ = new StringBuilder();

		packet_.append(method).append(" /").append(url).append(" HTTP/1.1\r\n");

		//packet_.append("Host: ").append("").append(host).append(":").append(port).append("\r\n");
		
		boolean first = true;
		for( String cookie: cookies_) {
			if( first ) {
				packet_.append("Cookie: ");
				first = false;
			}
			else {
				packet_.append("; ");
			}
			packet_.append(cookie);
		}
		if( !cookies_.isEmpty()) {
			packet_.append("\r\n");
		}
		
	}
	
	public void putHeader( String key, String value ) {
		packet_.append(key).append(": ").append(value).append("\r\n");
	}
	
	public void setBody( final String body ) {
		if( null == body || body.isEmpty() )
			return;
		
		packet_.append("Content-Type: application/x-www-form-urlencoded").append("\r\n");
		packet_.append("Content-Length: ").append((body.getBytes().length + 2)).append("\r\n").append("\r\n");
		packet_.append (body);
	}

	public void setBody( final byte[] body ) {
		if( null == body || 0 == body.length )
			return;
		
		packet_.append("Content-Type: application/x-www-form-urlencoded").append("\r\n");
		packet_.append("Content-Length: ").append((body.length + 2)).append("\r\n").append("\r\n");
		packet_.append (body);
	}
	
	public void end() throws IOException {
		end( 5 ); 
	}
	
	public void end( int timeoutSec ) throws IOException {
		
		packet_.append("\r\n");
		
		NioBlockClient client = new NioBlockClient();

		client.connect( host_, port_ );
		
		
		try {
			client.write(packet_.toString().getBytes());

			long beginTime = System.currentTimeMillis();
			
			byte[] head = null;
			
			while( true ) {

				head = client.readIf("\r\n\r\n".getBytes());
				if( null != head) {
					break;
				}

				if( (timeoutSec * 1000) < (System.currentTimeMillis() - beginTime) ) {
					throw new IOException("read timeout");
				}
			}
			
//			System.out.println( DumpBytes.str(head));

			response_ = HttpResponse.parse(ByteBuffer.wrap(head));
			if( null == response_ ) {
				throw new IOException("Failed to paring: \r\n" + DumpBytes.str(head));
			}


			String contentLen = response_.getHeaders().get("Content-Length");
			if( null != contentLen) {
				int len = Integer.parseInt(contentLen);

				body_ = client.read(len);
			}
			
			String transferEncoding = response_.getHeaders().get("Transfer-Encoding");
			if( null != transferEncoding && transferEncoding.equals("chunked")) {

				int totalLen = 0;
				ArrayList<byte[]> list = new ArrayList<byte[]>();
				
				while( true ) {
					
					byte[] chunkLen = client.readLine();
					if( chunkLen.length == 0 )
						break;
	
					int len = 0;
					try {
						len = Integer.parseInt(  (new String(chunkLen)).trim(), 16);
					}
					catch( NumberFormatException e ) {}

					if( 0 == len)
						break;

					byte[] data = client.read(len);

					client.read(2);
						
					list.add(data);
					totalLen += data.length;
				}
				
				body_ = new byte[totalLen];

				int pos = 0;
				for( byte[] data: list) {
					System.arraycopy( data, 0, body_, pos, data.length);
					pos += data.length;
				}

			}
			
			String cookie = response_.getHeaders().get("Set-Cookie");
			if( null != cookie) {
				cookies_.add( cookie.split(";")[0] );
			}
		}
		finally {
			client.disconnect();
		}
	}
	
	public String getPacket() {
		return packet_.toString();
	}
	
	public HttpResponse getResponse() {
		return response_;
	}
	
	public byte[] getBody() {
		return body_;
	}

	// public static void main(String[] args) {
	// 	test0();
	// }

	public static void test0() {
		HttpClient http;
		try {
			http = new HttpClient();
			/*
			String body = "login_id=admin&login_passwd=123123&url=";
			
			http.post("192.168.0.220", 9000, "login/login_admin_ok.jsp" );
			http.setBody(body);
			
			System.out.println(http.getPacket());
			
			http.end();

			System.out.println("Head: [" + http.getResponse() +"]");
			if( http.getBody() != null)
				System.out.println("Body: [" + new String(http.getBody()) +"]");

*/
			
//			http.get("192.168.0.220", 9000, "work/historylist.jsp", headers);
			http.get("192.168.0.220", 9000, "contract/monthlycontractmail.jsp?yyyymm=201412&bill_id=201412091739_135");
			http.putHeader( "Cookie", "JSESSIONID=4B3D467BA4C4189A8563DF9CB949DB68" );
		//	http.putHeader( "Content-Type", "text/html");
			http.putHeader( "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			http.putHeader( "Accept-Encoding", "gzip, deflate");
			http.putHeader( "Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
			http.putHeader( "Cache-Control", "max-age=0");
			http.putHeader( "Connection", "Keep-alive");
			http.putHeader( "User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
		

			System.out.println(http.getPacket());
			http.end();

			//http.get("192.168.0.220", 9000, "login/login_admin.jsp", headers);
			
			System.out.println("Head: [" + http.getResponse() +"]");
			if( http.getBody() != null)
				System.out.println("Body: [" + new String(http.getBody()) +"]");

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
}
