package hpr.net.umgp;

import hpr.util.KMPMatch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UmgpPacket {
	
	@SuppressWarnings("serial")
	public static class InvalidUmgpMethodException extends Exception {
		public InvalidUmgpMethodException(String string) {
			super(string);
		}
	}
	
	public static enum UmgpMethod { CONNECT, SEND, REPORT, ACK, PING, PONG }

	private UmgpMethod method_;
	private Map<String, String> headers_ = new HashMap<String, String>();
	
	public UmgpPacket( UmgpMethod method ) {
		method_ = method;
	}
	
	public UmgpMethod getMethod() {
		return method_;
	}
	
	public void setMethod( UmgpMethod method ) {
		method_ = method;
		headers_.clear();	
	}

	public String putHeader( String key, String value ) {
		return headers_.put( key, value );
	}
	
	public String getHeader( String key ) {
		return headers_.get( key );
	}
	
	public Map<String, String> getHeaders() {
		return headers_;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("method:").append(method_);
		str.append(" headers:").append(headers_);
			
		return str.toString();
	}
	
	public byte[] makePacket () {
		StringBuilder sb = new StringBuilder();
		
		sb.append("BEGIN ").append( method_.name()).append( "\r\n" );

		for (Entry<String, String> entry : headers_.entrySet() ) {
			if( entry.getKey().equals("DATA") ) {
				String datas[] = entry.getValue().split("\\r\\n");
				for (String data: datas) {
					sb.append( entry.getKey() ).append( ":" ).append( data ).append( "\r\n" );
				}
			}
			else {
				sb.append( entry.getKey() ).append( ":" ).append( entry.getValue() ).append( "\r\n" );
			}
		}

		sb.append("END").append( "\r\n" );
		
	//	System.out.println(sb.toString());
		return sb.toString().getBytes();
	}
	
	public static UmgpPacket parse (ByteBuffer buffer) throws InvalidUmgpMethodException {
		
		int pos = KMPMatch.indexOf (buffer.array(), buffer.position(), buffer.limit(), "\r\nEND\r\n".getBytes());
		if (-1 == pos) {
			return null;
		}
		
		UmgpPacket packet = null;
		
		
		byte[] buff = buffer.array();
		
		int beginPos = buffer.position();
		int length = buffer.limit();
		
	//	System.out.println("beginPos:" + beginPos + "," + length);
		//System.out.println("RAW:" + new String(buff, beginPos, pos-beginPos));	

		boolean isFirst = true;
		while (true) {
			
			//System.out.println("beginPos:" + beginPos + " length-beginPos:" + (length-beginPos));
			int endPos = KMPMatch.indexOf (buff, beginPos, length-beginPos, "\r\n".getBytes());
			if (-1 == endPos) {
				break;
			}

			//System.out.println("endPos:" + endPos);
			
			//System.out.println("beginPos:" + beginPos + " endPos-beginPos:" + (endPos-beginPos));
				
			int middlePos = KMPMatch.indexOf (buff, beginPos, endPos-beginPos, (isFirst ? " " : ":").getBytes());
			if (-1 == middlePos) {
				
				if (new String (buff, beginPos, endPos-beginPos).equals("END"))
					beginPos = endPos + 2;

				break;
			}
			//System.out.println("endPos:" + endPos + " middlePos:" + middlePos);
			
			String key 	= new String (buff, beginPos, middlePos-beginPos).trim();
			String value= new String (buff, middlePos+1, endPos-middlePos).trim();
			//System.out.println("key:" + key + " value:" + value);

			if (isFirst) {
				packet = new UmgpPacket( UmgpMethodStr (value));
				
				isFirst = false;
			}
			else {
				if( null == packet ) {
					throw new RuntimeException( "Parsing Error! It should be Bug!" );
				}
				packet.headers_.put(key, value);
			}
			beginPos = endPos + 2;
		}
		
		buffer.position(beginPos);

		return packet;
	}	
	
	public static UmgpMethod UmgpMethodStr( String method ) throws InvalidUmgpMethodException {
		for( int i = 0; i < UmgpMethod.values().length; ++i ) {
			if( UmgpMethod.values()[i].name().equals(method)) {
				return UmgpMethod.values()[i];
			}
		}
		throw new InvalidUmgpMethodException("Unknown method: " + method);
	}
}
