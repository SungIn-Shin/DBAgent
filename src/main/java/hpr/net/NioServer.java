package hpr.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioServer {

	private ServerSocketChannel server_ = null;
	
	private NioSelect nioSelect_;

	public NioServer( NioSelect nioSelect ) {
		nioSelect_ = nioSelect;
	}
	
	public void bind (int port) throws IOException {
		server_ = ServerSocketChannel.open();

		server_.socket().bind(new InetSocketAddress(port));
		server_.configureBlocking(false);

		nioSelect_.regist ( server_ );
	}
	
}
