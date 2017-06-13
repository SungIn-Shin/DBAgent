package hpr.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public final class NioSelect extends AbstractNioSelect {

	private INioSelect callback_;
	
	public NioSelect(INioSelect callback) throws IOException {
		super();
		callback_ = callback;
	}

	public void regist ( SocketChannel sc, INioSession event) {
		super.regist( sc, event);
	}	
	
	public void regist ( ServerSocketChannel ssc ) {
		super.regist( ssc );
	}	
	
	@Override
	protected void onAccept(SelectionKey key) {
		SocketChannel sc = (SocketChannel)key.channel();
		NioSession session = new NioSession( this, sc );
		
		INioSession event = callback_.onAccept( session );
		if( null != event ) {
			key.attach(event);
		}
	}

	@Override
	protected void onConnect(SelectionKey key, IOException ex) {
		INioClient event = (INioClient)key.attachment();
		event.onConnect(ex);
		
	}

	@Override
	protected void onDisconnected(SelectionKey key) {
		INioSession event = (INioSession)key.attachment();
		event.onDisconnected();
	}

	@Override
	protected void onException(SelectionKey key, IOException ex) {
		INioSession event = (INioSession)key.attachment();
		event.onException(ex);
	}

	@Override
	protected void onRead(SelectionKey key ) {
		INioSession event = (INioSession)key.attachment();
		event.onRead(key);
	}

	@Override
	protected void onException(Exception ex) {
		callback_.onException( ex );
	}
}
