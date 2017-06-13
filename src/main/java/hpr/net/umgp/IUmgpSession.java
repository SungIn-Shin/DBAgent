package hpr.net.umgp;

public interface IUmgpSession {

	void onUmgpReqConnect( UmgpPacket packet );
	void onUmgpReqSend( UmgpPacket packet );
	void onUmgpReqReport( UmgpPacket packet );
	void onUmgpReqAck( UmgpPacket packet );
	void onUmgpReqPing( UmgpPacket packet );
	void onUmgpResPong( UmgpPacket packet );
	void onUmgpException( Exception ex );
	
}
