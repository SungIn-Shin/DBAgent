package hpr.net;

import java.io.IOException;

public interface INioClient extends INioSession {
	public void onConnect( IOException  ex );
}
