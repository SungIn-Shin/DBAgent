package hpr.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioSession {
	
	protected SocketChannel socketChannel_ = null;
	private NioSelect nioSelect_;

	public NioSession( NioSelect nioSelect, SocketChannel socketChannel ) {
		this( nioSelect );
		
		socketChannel_ = socketChannel;
	}
	
	protected NioSession( NioSelect nioSelect ) {
		if( null == nioSelect )
			throw new RuntimeException( "nioSelect should not be NULL");
		nioSelect_ = nioSelect;
	}
	
	
	public SocketChannel getSocketChannel() {
		return socketChannel_;
	}
	
	
	public void disconnect () throws IOException {
		if( null != socketChannel_ )
			socketChannel_.close();
	}
	
	public int send( byte[] data ) {
		return send( data, data.length );
	}
		
	public int send( byte[] data, int length ) {
		if( null == socketChannel_)
			return -1;
		
		return nioSelect_.send( socketChannel_, data, length );
	}

	public ByteBuffer getRecvBuff( final int buffSize ) {
		return nioSelect_.getRecvBuff( socketChannel_, buffSize );
	}

	
	public void putRecvBuff( ByteBuffer buffer ) {
		nioSelect_.putRecvBuff( socketChannel_, buffer );
	}

	public void cancel() {
		if( null != socketChannel_ )
			nioSelect_.cancel(socketChannel_);
	}
	
	public int read ( ByteBuffer buffer ) throws IOException {
		return nioSelect_.read( socketChannel_, buffer );
	}
}
