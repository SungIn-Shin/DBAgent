package hpr.net.http;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface IHttpSend {
	public void send( SocketChannel sc, ByteBuffer buffer);

}
