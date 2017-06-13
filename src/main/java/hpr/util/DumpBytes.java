package hpr.util;

import java.nio.ByteBuffer;

public class DumpBytes {
/*	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
*/	

	private final static int base = 16;

	public static String str( byte[] arg ) {
		return str( arg, arg.length );
	}
	
	public static String str( byte[] arg, int length ) {
		if( arg.length < length )
			length = arg.length;
		
		StringBuilder sb = new StringBuilder();
		
		for( int roff = 0; roff < (length + base - 1) / base; ++roff ) {
			StringBuilder sbHEX = new StringBuilder();
			StringBuilder sbTXT = new StringBuilder();

			sbHEX.append( String.format( "[%04X] :", roff * base) );
			
			for( int coff = 0; coff < base; ++coff) {
				int off = roff * base + coff;
				if( off < length ) {
					sbHEX.append( String.format( "%02X ", arg[off]) );

					if( (coff % 4) == 3 ) {
						sbHEX.append( " " );
					}

					if( arg[off] < 20 ) {
						sbTXT.append( '.' );
					}
					else {
						sbTXT.append( (char)arg[off] );
					}
				}
				else {
					if( (coff % 4) == 3 ) {
						sbHEX.append( " " );
					}
					sbHEX.append( "   " );
				}
			}
			sb.append( sbHEX ).append(" ").append( sbTXT ).append( "\n" );
		}
		
		return sb.toString();
	}
	

	
	// public static void main(String[] args) {
	// 	ByteBuffer b = ByteBuffer.allocate(128);
		
	// 	b.putInt(1);
	// 	b.putInt(2);
	// 	b.putInt(3);
	// 	b.putInt(4);
	// 	b.put("abcdefg".getBytes());
		
		
	// 	System.out.println(DumpBytes.str( b.array(), b.array().length));
	
	// }
	
}
