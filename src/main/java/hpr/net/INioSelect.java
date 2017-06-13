package hpr.net;


public interface INioSelect {
	public INioSession onAccept(NioSession session) ;
	public void onException(Exception ex) ;
}
