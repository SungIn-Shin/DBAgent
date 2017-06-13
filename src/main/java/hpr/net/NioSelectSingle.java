package hpr.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public final class NioSelectSingle extends AbstractNioSelect {

	private INioSelectSingle callback_;
	
	public NioSelectSingle(INioSelectSingle callback) throws IOException {
		super();
		callback_ = callback;
	}

	public void regist ( SocketChannel sc ) {
		super.regist( sc, null );
	}	
	
	public void regist ( ServerSocketChannel ssc ) {
		super.regist( ssc );
	}	
	
	@Override
	protected void onAccept(SelectionKey key) {
		callback_.onAccept( key );
	}

	@Override
	protected void onConnect(SelectionKey key, IOException ex) {
		callback_.onConnect(key, ex);
	}

	@Override
	protected void onDisconnected(SelectionKey key) {
		callback_.onDisconnected(key);
	}

	@Override
	protected void onException(SelectionKey key, IOException ex) {
		callback_.onException(key, ex);
	}

	@Override
	protected void onRead(SelectionKey key ) {
		callback_.onRead( key );
	}

	@Override
	protected void onException(Exception ex) {
		callback_.onException( ex );
	}
}
