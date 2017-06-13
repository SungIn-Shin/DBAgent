package hpr.net;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class AbstractNioSelect {
	static final private int CANCEL	= -1;

	//private int DEFAILT_BUFFER_SIZE = 100;
	//private int DEFAILT_BUFFER_SIZE = 8192;
	
	private	Selector selector_ = null;

	private Map<SocketChannel, List<ByteBuffer>> sendBufferMap_;
	private Map<SocketChannel, ByteBuffer> recvBufferMap_;
	
	static private class InterestOps {
		private int op_;
		private SocketChannel sc_;
		private ServerSocketChannel ssc_;
		private Object ob_;
	};
	
	private List<InterestOps> interestOpsQue_ = new LinkedList<InterestOps>();
	
	abstract protected void onConnect( SelectionKey key, IOException ex );
	abstract protected void onAccept ( SelectionKey key );
	abstract protected void onDisconnected( SelectionKey key );
	abstract protected void onRead( SelectionKey key );
	abstract protected void onException ( SelectionKey key, IOException ex);
	abstract protected void onException ( Exception ex );
	
	public AbstractNioSelect ()  throws IOException {
		
		super();
		
		selector_ = SelectorProvider.provider().openSelector();
		
		recvBufferMap_ = new HashMap<SocketChannel, ByteBuffer>();
		sendBufferMap_ = new HashMap<SocketChannel, List<ByteBuffer>>();
	}
	
	public boolean hasSendData( SocketChannel sc ) {
		synchronized (sendBufferMap_) {
			List<ByteBuffer> list = sendBufferMap_.get (sc);
			if( null == list || list.isEmpty() ) {
				return false;
			}
			return true;
		}
	}
	
	public int send( SocketChannel sc, byte[] data, int length ) {
		return send( sc, ByteBuffer.wrap (data, 0, length ));
	}
	
	public int send( SocketChannel sc, ByteBuffer data ) {
		int cnt = 0;
		synchronized (sendBufferMap_) {
			List<ByteBuffer> list = sendBufferMap_.get( sc );
			if( null == list ) {
				list = new ArrayList<ByteBuffer>();
			}
			list.add( data );
			cnt = list.size();
			
			sendBufferMap_.put (sc, list);
		}
	
		interestOps (sc, SelectionKey.OP_WRITE);

		selector_.wakeup();
		
		return cnt;
	}	
	
	public int read ( SocketChannel sc, ByteBuffer buffer ) throws IOException {
		int readBytes = sc.read(buffer);
//		int readBytes = ((ReadableByteChannel )key.channel()).read(buffer);
//		System.out.println("doRead read:" + readBytes);
		if (-1 == readBytes) {
			
			removeKey( sc.keyFor(selector_) );
		}
	
		buffer.flip();
		
		return readBytes;
	}
	

	public ByteBuffer getRecvBuff( SocketChannel sc ) {
		return recvBufferMap_.get(sc);
	}

	public ByteBuffer getRecvBuff( SocketChannel sc, final int buffSize ) {
		ByteBuffer buffer = getRecvBuff(sc);
		if (null == buffer) {
			buffer = ByteBuffer.allocate(buffSize);
			recvBufferMap_.put(sc, buffer);
		}
		
		return buffer;
	}
	
	public void putRecvBuff( SocketChannel sc, ByteBuffer buffer ) {
		recvBufferMap_.put(sc, buffer);
	}
	
	protected void cancel (SocketChannel sc) {
		
		InterestOps ops = new InterestOps();
		
		ops.op_ = CANCEL;
		ops.sc_ = sc;
		ops.ssc_= null;

		synchronized (interestOpsQue_) {
			interestOpsQue_.add( ops );
		}
	}

	private void removeKey (SelectionKey key) {
		
		if (null == key || !key.isValid())
			return;

		SocketChannel sc = (SocketChannel)key.channel();

		this.onDisconnected( key );
		
		recvBufferMap_.remove(sc);
		synchronized (sendBufferMap_) {
			
			List<ByteBuffer> que = sendBufferMap_.get( sc );
			if( null != que )
				que.clear();

			sendBufferMap_.remove( sc );
		}
		
		try {
			sc.close();
		} catch (IOException e) {}
		
		key.cancel();
	}		
	
	private void interestOps ( SocketChannel sc, int op ) {
		
		if( SelectionKey.OP_READ != op && SelectionKey.OP_WRITE != op )
			throw new UnsupportedOperationException( "Invalid SelectionKey type: " + op );
		
		InterestOps ops = new InterestOps();
		
		ops.op_ = op;
		ops.sc_ = sc;

		synchronized (interestOpsQue_) {
			interestOpsQue_.add( ops );
		}
	}

	protected void regist ( SocketChannel sc, Object ob ) {
		
		InterestOps ops = new InterestOps();
		
		ops.op_ = SelectionKey.OP_CONNECT;
		ops.sc_ = sc;
		ops.ssc_= null;
		ops.ob_ = ob;

		synchronized (interestOpsQue_) {
			interestOpsQue_.add( ops );
		}
	}
	
	protected void regist ( ServerSocketChannel ssc ) {
		
		InterestOps ops = new InterestOps();
		
		ops.op_ = SelectionKey.OP_ACCEPT;
		ops.sc_ = null;
		ops.ssc_= ssc;

		synchronized (interestOpsQue_) {
			interestOpsQue_.add( ops );
		}
	}	
	
	private void popInterestOps() {
		synchronized (interestOpsQue_) {
			Iterator<InterestOps> it = interestOpsQue_.iterator();
			while( it.hasNext() ) {
				InterestOps req = it.next();

				try {
					if( CANCEL == req.op_ ) {
						try {
							req.sc_.socket().shutdownInput();
						}
						catch (IOException e) {
							// Ignore exception when this socket had already closed
						}
					}
					else if( SelectionKey.OP_CONNECT == req.op_ ) {
						SelectionKey key = req.sc_.register( selector_, req.op_);
						key.attach(req.ob_);
					}
					else if( SelectionKey.OP_ACCEPT == req.op_ ) {
						req.ssc_.register( selector_, req.op_);
					}
					else { 
						SelectionKey key = req.sc_.keyFor( selector_ );
						key.interestOps( req.op_ );
					}
				}
				catch (IOException e) {
					this.onException( e );
				}
			}
			interestOpsQue_.clear();
		}
	}
	
	
	private void doConnect( SelectionKey key ) {

		SocketChannel sc = (SocketChannel)key.channel();

		try {
			
			if( sc.isConnectionPending() ) {
				boolean res = sc.finishConnect();
	//			System.out.println("RES:" + (res ? "T" : "F"));
			} 
		
			interestOps (sc, SelectionKey.OP_READ);
		//	interestOps (sc, SelectionKey.OP_WRITE);
		//	key.interestOps( SelectionKey.OP_WRITE);
	
			this.onConnect( key, null);
		} 
		catch (IOException e) {
			this.onConnect( key, e );
			key.cancel();
		}
	}	
	
	private void doAccept( SelectionKey key ) {
		
		ServerSocketChannel sc = (ServerSocketChannel)key.channel();
		
		try {
			SocketChannel client = sc.accept();
			client.configureBlocking(false);
			client.socket().setTcpNoDelay(true);
			SelectionKey clientKey = client.register( selector_, SelectionKey.OP_READ);

			this.onAccept ( clientKey );
	
		} catch (IOException e) {
			this.onException( e );
		}		
	}
	
	private void doRead( SelectionKey key ) throws IOException {
//		System.out.println("doRead");
		
/*
		SocketChannel sc = (SocketChannel)key.channel();

		ByteBuffer buffer = recvBufferMap_.get(sc);
		if (null == buffer) {
			buffer = ByteBuffer.allocate(DEFAILT_BUFFER_SIZE);
			recvBufferMap_.put(sc, buffer);
		}
		
		int readBytes = sc.read(buffer);
//		int readBytes = ((ReadableByteChannel )key.channel()).read(buffer);
//		System.out.println("doRead read:" + readBytes);
		if (-1 == readBytes) {
			removeKey( key );
			return;
		}
	
		buffer.flip();
//		System.out.println("A:" + buffer);
*/		
		this.onRead( key );
	/*	
		if (!res) {

			if (buffer.limit() == buffer.capacity()) {
				int capacity = buffer.capacity();
				System.out.println("capacity * 2: " + capacity * 2);
				ByteBuffer largeBuffer = ByteBuffer.allocate(capacity * 2); // Must have max length
				buffer.position(0);
				largeBuffer.put(buffer);
				largeBuffer.position(capacity);
				largeBuffer.limit(largeBuffer.capacity());

				recvBufferMap_.put(sc, largeBuffer);
			}
			else {
				buffer.position (buffer.limit());
				buffer.limit(buffer.capacity());
			}
//			System.out.println("D:" + buffer);
		}
*/
	}   
	
	private void doWrite( SelectionKey key ) throws IOException {
//		System.out.println("doWrite");
		
		SocketChannel sc = (SocketChannel)key.channel();

		synchronized (sendBufferMap_) {
			
			List<ByteBuffer> que = sendBufferMap_.get( sc );
		
			
			if( null == que) {
			//	throw new IOException( "Nothing to write. What happen?" );
			}
			
			
			while (null != que && !que.isEmpty()) {
				ByteBuffer buffer = que.get(0);
//				System.err.println("Send");

				int sent = sc.write( buffer );
				//System.err.println("Sent:"+sent);
				if( 0 < buffer.remaining()) {
					break;
				}
				que.remove(0);
			}
			
			if (null == que || que.isEmpty()) {
				interestOps( sc, SelectionKey.OP_READ );
			}			
		}
	}
	
	
	public boolean work() {
		return work( 1000 );
	}
	
	public boolean work( int waitInterval ) {

		popInterestOps();
		
		try {
			selector_.select(waitInterval);
		} catch (IOException ex) {
			this.onException( ex );

			return true;
		}

		Iterator<SelectionKey> it = selector_.selectedKeys().iterator();
		if( !it.hasNext() )
			return false;

		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();
//				System.out.println("...***...");

			if( !key.isValid() ) {
				continue;
			}

			try {
				if( key.isConnectable() ) {
					doConnect( key );
				}
				else if( key.isAcceptable() ) {
					doAccept( key );
				}
				else if( key.isReadable() ) {
					doRead( key );
				}
				else if( key.isWritable() ) {
					doWrite( key );
				}
				else {
					throw new IOException( "Invalid key type" );
				}
			}
			catch (IOException ex) {
				//SocketChannel sc = (SocketChannel)key.channel();
				this.onException( key, ex );
				removeKey (key);
			}
		}
		
		return true;
	}

}