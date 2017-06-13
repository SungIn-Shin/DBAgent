package hpr.net.http;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface IHttpServer {
	public void onAccept ( SocketChannel sc );
	public void onDisconnected( SocketChannel sc );
	public void onHttp( HttpRequest req, HttpResponse res );
	public void onException ( SocketChannel sc, IOException ex);
	public void onException ( Exception ex );
}
