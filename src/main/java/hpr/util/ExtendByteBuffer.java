package hpr.util;

import java.nio.ByteBuffer;

public class ExtendByteBuffer {

	
	public static ByteBuffer extend ( ByteBuffer srcBuff, final int targetSize, final int maxBuffSize ) throws Exception {

		int pos = srcBuff.position();
		if( pos + targetSize <= srcBuff.limit()) {
			return srcBuff;
		}

		int extendSize = srcBuff.limit();

		while( true ) {
			
			extendSize = extendSize * 2;
			if( extendSize > maxBuffSize ) {
				extendSize = maxBuffSize;
				break;
			}
			
			if( pos + targetSize <= extendSize) {
				break;
			}
		}

		if( pos + targetSize > extendSize ) {
			throw new Exception("Failed to extend buffer. extendSize:" + extendSize);
		}
		
		ByteBuffer largeBuffer = ByteBuffer.allocate(extendSize); 
		srcBuff.position(0);
		largeBuffer.put(srcBuff);
		largeBuffer.position(pos);
		largeBuffer.limit(largeBuffer.capacity());
		
		return largeBuffer;
	}
	
	// public static void main(String[] args) {
	// 	// TODO Auto-generated method stub
		
	// 	ByteBuffer srcBuff = ByteBuffer.allocate(100);
		
	// 	srcBuff.clear();
	// 	try {
	// 		System.out.println(srcBuff);
	// 		srcBuff = extend(srcBuff, 1290, 1000);
	// 		System.out.println(srcBuff);
			
	// 	} catch (Exception e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}
		
		

	// }

}
