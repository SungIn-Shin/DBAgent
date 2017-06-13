package hpr.net.http;


import hpr.util.KMPMatch;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;



public class HttpRequest {
	
	private String method_;
	private URI uri_;
	private Map<String, String> headers_;
	public byte[] body_;

	public String getMethod() {
		return method_;
	}
	
	public URI getUri() {
		return uri_;
	}
	
	public Map<String, String> splitQuery() throws UnsupportedEncodingException {
		if( null == uri_ || null == uri_.getQuery() ) {
			return new HashMap<String, String>();
		}
		
		return HttpRequest.SplitQuery( uri_.getQuery() );
	}

	public Map<String, String> splitQuery( String charSet ) throws UnsupportedEncodingException {
		if( null == uri_ || null == uri_.getQuery() ) {
			return new HashMap<String, String>();
		}

		return HttpRequest.SplitQuery( uri_.getQuery(), charSet );
	}
	
	public Map<String, String> splitCookie() {
		String cookieStr = getHeaders().get("Cookie");
		
		if( null == cookieStr || cookieStr.isEmpty()) {
			return new HashMap<String, String>();
		}
		
		Map<String, String> cookie_pairs = new HashMap<String, String>();
	    String[] cookies = cookieStr.split(";");
	    for (String cookie : cookies) {
	    	String pair = cookie.trim();
	    	
	        int idx = pair.indexOf("=");
	        
	        String key = "";
	        String value = "";
	        
	        if( -1 != idx) {
	        	key = pair.substring(0, idx);
	        	value = pair.substring(idx + 1);
	        }
	        else {
		        key = pair;
	        }
	        
	        cookie_pairs.put(key, value);
	    }
	    return cookie_pairs;
	}
	
	public byte[] getBody() {
		return body_;
	}
	
	public Map<String, String> getHeaders() {
		return headers_;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("method:").append(method_);
		str.append(" uri:").append(uri_);
		str.append(" headers:").append(headers_);
			
		return str.toString();
	}
	
	static public HttpRequest parse (ByteBuffer buffer) throws URISyntaxException {
		
		
		int pos = KMPMatch.indexOf (buffer.array(), buffer.position(), buffer.limit(), "\r\n\r\n".getBytes());
		if (-1 == pos) {
			return null;
		}
		
		HttpRequest packet = new HttpRequest();
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
				packet.method_ = new String (key);
				
				int urlPos = KMPMatch.indexOf (value.getBytes(), 0, value.getBytes().length, " ".getBytes());
				if (-1 != urlPos) {
					String url = new String (value.substring(0, urlPos));
						
				//	try {
						packet.uri_ = new URI(url);
				//	}
				//	catch (URISyntaxException ex) {
						// 파싱 에런데 그걸 전파할 필요가 있을까?
						//throw new Exception (ex.getMessage());
				//	}
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
	
	public static Map<String, String> parseQuery (String query) {
	
		Map<String, String> params = new HashMap<String, String>();
	
		String paramList[]  = query.split ("&");
		for (String param : paramList) {
			String keyValue[]  = param.split ("=", 2);

			if (keyValue.length == 2) {
				params.put(keyValue[0], keyValue[1]);
			}
		}
		
		return params;
	}
	
	public static Map<String, String> SplitQuery( String query ) throws UnsupportedEncodingException {

//		try {
			return SplitQuery( query, null );
//		} catch (UnsupportedEncodingException e) {}
		
//		throw new RuntimeException("Framework Bug");
	}

	
	public static Map<String, String> SplitQuery( String query, String charSet ) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new HashMap<String, String>();
	    String[] pairs = query.split("&");
	    for (String pair : pairs) {
	        int idx = pair.indexOf("=");
	        
	        String key = "";
	        String value = "";
	        
	        if( -1 != idx) {
	        	key = pair.substring(0, idx);
	        	value = pair.substring(idx + 1);
	        }
	        else {
		        key = pair;
	        }
	        
	        if (null != charSet && !charSet.isEmpty()) {
	        	key = URLDecoder.decode(key, charSet);
	        	value = URLDecoder.decode(value, charSet);
	        }
	        query_pairs.put(key, value);
	    }
	    return query_pairs;
	}
}
