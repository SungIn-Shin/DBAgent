package hpr.net.umgp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import hpr.net.INioClient;
import hpr.net.INioSelect;
import hpr.net.INioSession;
import hpr.net.NioClient;
import hpr.net.NioSelect;
import hpr.net.NioSession;
import hpr.net.umgp.UmgpPacket.InvalidUmgpMethodException;
import hpr.net.umgp.UmgpPacket.UmgpMethod;
import hpr.util.ExtendByteBuffer;
import hpr.util.StateThread;

public class UmgpSession {

    public enum LineType {SEND_LINE, REPORT_LINE}

	private NioSession session_ = null;
    private IUmgpSession callback_ = null;
    private LineType lineType_;
	
	public UmgpSession( NioSession session, IUmgpSession callback ) {
		session_ = session;
		callback_ = callback;
	}
	
    public void setLineType( LineType lineType ) {
    	lineType_ = lineType;
    }

    public LineType getLineType() {
    	return lineType_;
    }
    
    public NioSession getNioSession() {
    	return session_;
    }

    public UmgpPacket reqConnect( String id, String pwd) {
    	
    	if( null == lineType_) {
    		throw new RuntimeException("lineType should not be null");
    	}
    	
    	UmgpPacket packet = new UmgpPacket( UmgpMethod.CONNECT );

    	packet.putHeader( "ID"			,  id );
    	packet.putHeader( "PASSWORD"	,  pwd );
    	packet.putHeader( "REPORTLINE"	,  lineType_ == LineType.REPORT_LINE ? "Y" : "N" );
    	packet.putHeader( "VERSION"		,  "HPR/UMGP 0.1" );

    	session_.send( packet.makePacket() );
    	
    	return packet;
    }
	
    public UmgpPacket reqSend( String key, String receiverNum, String callback, String data ) {

    	if( null == lineType_ || lineType_ != LineType.SEND_LINE ) {
    		throw new RuntimeException( "reqSend is only for SEND_LINE" );
    	}
    	
    	UmgpPacket packet = new UmgpPacket( UmgpMethod.SEND );

    	packet.putHeader( "KEY"			,  key );
    	packet.putHeader( "RECEIVERNUM"	,  receiverNum );
    	packet.putHeader( "CALLBACK"	,  callback );
    	packet.putHeader( "DATA"		,  data );

    	session_.send( packet.makePacket() );
    	
    	return packet;
    }
    
    public UmgpPacket resAck (String key, String code, String data) {

    	if( null == lineType_) {
    		throw new RuntimeException("lineType should not be null");
    	}

    	UmgpPacket packet = new UmgpPacket( UmgpMethod.ACK );

    	packet.putHeader( "KEY"	,  key );
    	packet.putHeader( "CODE",  code );
    	packet.putHeader( "DATA",  data );

    	session_.send( packet.makePacket() );
    	
    	return packet;
    }    
    
    public void reqPing (String key) {

    	if( null == lineType_) {
    		throw new RuntimeException("lineType should not be null");
    	}

    	UmgpPacket packet = new UmgpPacket( UmgpMethod.PING );

    	packet.putHeader( "KEY",  key );

    	session_.send( packet.makePacket() );
    }

    public void resPong (String key) {

    	if( null == lineType_) {
    		throw new RuntimeException("lineType should not be null");
    	}

    	UmgpPacket packet = new UmgpPacket( UmgpMethod.PONG );

    	packet.putHeader( "KEY",  key );

    	session_.send( packet.makePacket() );
    }

    public void cancel() {
    	session_.cancel();
    }

    public boolean read (SelectionKey key) {
    	
		 ByteBuffer buffer = session_.getRecvBuff(1024);//defaultRecvBuffSize_);

		 boolean res = false;
		 try {

			 int readBytes = session_.read( buffer );

			 if( 0 >= readBytes) {
				 
				// buffer.compact();
				 return false;
			 }
			 
			 while(true) {
				 
				 res = readPacket( buffer );
	
				 if( res ) {
					 buffer.compact();
					 buffer.flip();
				 }
				 else {
					 buffer.position (buffer.limit());
					 buffer.limit(buffer.capacity());
	
					 if( buffer.position() == buffer.capacity()) {
						 buffer = ExtendByteBuffer.extend(buffer, buffer.capacity() * 2, 4096000);//maximumRecvBuffSize_);
	
						 session_.putRecvBuff(buffer);
					 }
					 break;
				 }
			 }
		 } catch (Exception ex) {
			 
			 callback_.onUmgpException( ex );	
			 session_.cancel();
		 }
		 
		 return res;
    }
        

    private boolean readPacket(ByteBuffer buffer) throws InvalidUmgpMethodException {

    	UmgpPacket packet = UmgpPacket.parse(buffer);
		if( null == packet ) {
			return false;
		}
		
		switch( packet.getMethod() ) {
		case CONNECT:
			if( "N".equalsIgnoreCase(packet.getHeader("REPORTLINE"))) {
				lineType_ = LineType.SEND_LINE;
			}
			else if( "Y".equalsIgnoreCase(packet.getHeader("REPORTLINE"))) {
				lineType_ = LineType.REPORT_LINE;
			}
			else {
	    		throw new InvalidUmgpMethodException( "REPORTLINE must be Y or N. but [" + packet.getHeader("REPORTLINE") + "]");
			}
	
			callback_.onUmgpReqConnect( packet );
			
	    	break;
			
		case SEND:
			if( lineType_ != LineType.SEND_LINE ) {
	    		throw new InvalidUmgpMethodException( "SEND is only for SEND_LINE" );
	    	}
			
			callback_.onUmgpReqSend( packet );
			break;
			
		case REPORT:
			if( lineType_ != LineType.REPORT_LINE ) {
	    		throw new InvalidUmgpMethodException( "REPORT is only for REPORT_LINE" );
	    	}
			callback_.onUmgpReqReport( packet );
			break;
			
		case PING:
			callback_.onUmgpReqPing( packet );
			resPong( packet.getHeader("KEY") );
			break;
		case PONG:
			callback_.onUmgpResPong( packet );
			break;
	
		case ACK:
			callback_.onUmgpReqAck( packet );
			break;

		default:
			throw new InvalidUmgpMethodException("Unknown UMGP method:" + packet.getMethod());
		}
		
    	return true;	
    }

    

// 	public static void main(String[] args) {
		
// 		class Client extends StateThread implements INioSelect, INioClient, IUmgpSession {

// 			public NioSelect select_;
// 			public NioClient client_;
// 			public UmgpSession umgp_;
			
// 			public Client() throws IOException {
// 				select_ = new NioSelect(this);
// 				client_ = new NioClient( select_, this );
// 				umgp_ = new UmgpSession( client_, this );
// 			}

			
// 			@Override
// 			public void onStart() {
// 				System.out.println("thread start");
// 			}

// 			@Override
// 			public void onStop() {
// 				System.out.println("thread stop");
// 			}

// 			@Override
// 			public INioSession onAccept(NioSession session) {
// 				throw new RuntimeException("Fuck");
// 			}

// 			@Override
// 			public void onException(Exception ex) {
// 				System.out.println( "EXCEPTION:" + ex);
// 				ex.printStackTrace();
// 			}

			
// 			@Override
// 			public void onDisconnected() {
// 				System.out.println( "connection is disconnected ");
// //				umgp_ = null;
// 			}
			


// 			@Override
// 			public void onRead(SelectionKey key) {
// 				// TODO Auto-generated method stub
				
// 			}
// /*
// 			@Override
// 			public boolean onRead(ByteBuffer buffer) {
		
// 				while (true) {
// 		    		boolean isContinue = false;
		 		
// 		    		try {
// 						isContinue = umgp_.readPacket( buffer );
// 					} catch (InvalidUmgpMethodException ex) {
// 						System.out.println("Invalid Umgp Method:" + Except.getStackTrace(ex));
// 					}
					
// 		    		if( !isContinue )
// 		    			break;
// 		    	}
// 		    	return (buffer.position() == buffer.limit());			
// 			}
// */
// 			@Override
// 			public void onException(IOException ex) {
// 				System.out.println( "IOException:" + ex.getMessage());
// 				ex.printStackTrace();
// 			}

// 			@Override
// 			public void onConnect(IOException ex) {
// 				if( ex == null ) {
// 					System.out.println( "connection is success");
					
// 					UmgpPacket packet = umgp_.reqConnect("hopper", "hopper" );
// 					System.out.println( "[REQ] " + packet);
				
// 				}
// 				else {
// 					System.out.println( "Failed to connect! : " + ex.getMessage());
// 				}				
// 			}

// 			@Override
// 			public void onUmgpReqConnect(UmgpPacket packet) {
// 				throw new RuntimeException("Fuck");
// 			}

// 			@Override
// 			public void onUmgpReqSend(UmgpPacket packet) {
// 				throw new RuntimeException("Fuck");
// 			}

// 			@Override
// 			public void onUmgpReqReport(UmgpPacket packet) {
// 				System.out.println("R [REQ] " + packet.toString());	
// 				UmgpPacket p = umgp_.resAck(packet.getHeader("KEY"), "100", "");
// 				System.out.println("R [RES] " + p.toString());	
// 			}

// 			@Override
// 			public void onUmgpReqAck(UmgpPacket packet) {
// 				System.out.println("[RES] " + packet.toString());				
// 			}

// 			@Override
// 			public void onUmgpReqPing(UmgpPacket packet) {
// 				System.out.println("onUmgpReqPing:" + packet.toString());				
// 			}

// 			@Override
// 			public void onUmgpResPong(UmgpPacket packet) {
// 				System.out.println("onUmgpResPong:" + packet.toString());				
// 			}


// 			@Override
// 			public void onUmgpException(Exception ex) {
// 				System.out.println( "onUmgpException:" + ex.getMessage());
// 				ex.printStackTrace();
// 			}


// 			@Override
// 			protected boolean onRun() {
// 				select_.work();
// 				return true;
// 			}

// 		}
		
// 		try {
// 			Client c = new Client();

// 			c.start();
			
// 			//c.umgp_.setLineType(LineType.SEND_LINE);
// 			c.umgp_.setLineType(LineType.REPORT_LINE);
// 			c.client_.connect("211.239.159.203", 4000);

// 			Thread.sleep(3000);
// 			UmgpPacket packet = c.umgp_.reqSend("KEY_123", "01011112222", "1004", "TEST");
// 			System.out.println("[REQ] " + packet.toString());				

			
// 			Thread.sleep(3000 * 10);
// 			c.client_.disconnect();

// 			c.stop();

// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		} catch (InterruptedException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
		
// 	}
	
}
