package hpr.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface INioSelectSingle {

	public void onConnect( SelectionKey key, IOException ex );
	public void onAccept ( SelectionKey key );
	public void onDisconnected( SelectionKey key );
	public void onRead( SelectionKey key );
	public void onException ( SelectionKey key, IOException ex);
	public void onException ( Exception ex );
}
