package hpr.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface INioSession {

	public void onDisconnected();
	public void onRead(SelectionKey key);
	public void onException ( IOException ex);
}
