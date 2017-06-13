package hpr.que.bptree;

import hpr.que.filedb.BPTree;
import hpr.que.filedb.BPTree.DiskMode;
import hpr.util.Tuple;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.Stack;

public class BPlusTree extends BPTree {

	public BPlusTree (int keySize, int valueSize, DiskMode mode ) {
		super(keySize, valueSize, mode );
	}
	
	public BPlusTree (int keySize, int valueSize, DiskMode mode, int pageSize ) {
		super(keySize, valueSize, mode, pageSize );
	}
	
	public synchronized void createOpen( String pathName, String fileName ) throws IOException {

		try {
			open( pathName, fileName );
			return;
		}
		catch (IOException ex) {}

		create( pathName, fileName );
	}

	public synchronized boolean insert( final byte[] key, final byte[] value ) throws IOException {
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
			
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( !ret.get1() ) {
				return super.insertRowid( key, value, ret.get2(), ret.get3() );
			}
		}
		finally {
			releaseLock( lock );
		}
		return false;
	}
	
	public synchronized byte[] select( final byte[] key ) throws IOException {

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
		
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( ret.get1() ) {
				BPTreePage page = ret.get2();
				Stack<StackData> stack = ret.get3();
				
				return page.getValue( stack.pop().index_ );
			}
		}
		finally {
			releaseLock( lock );
		}
	
		return new byte[0];
	}

	public synchronized boolean delete ( final byte[] key ) throws IOException {

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
			
			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( !ret.get1() ) {
				return false;
			}
			
			BPTreePage page = ret.get2();
			Stack<StackData> stack = ret.get3();
			
			super.deleteRowid( key, page, stack );
		}
		finally {
			releaseLock( lock );
		}
		return true;
	}
	
	public synchronized byte[] selectAndDelete ( final byte[] key ) throws IOException {
		byte[] value = new byte[0];
		
		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();

			Tuple<Boolean, BPTreePage, Stack<StackData>> ret = lookUpNode( key );
			if( !ret.get1() ) {
				return value;
			}

			BPTreePage page = ret.get2();
			Stack<StackData> stack = ret.get3();

			value = page.getValue( stack.pop().index_ );

			super.deleteRowid( key, page, stack );
		}
		finally {
			releaseLock( lock );
		}
		return value;
	}
	
	public synchronized StringBuilder display() throws IOException {
		
		StringBuilder sb = new StringBuilder();

		FileLock lock = getLock();
		try {
			mainHeader_.readHeader();
			BPTreePage page = new BPTreePage( mainHeader_.pageIdxRoot_ );
			page.display( sb );
		}
		finally {
			releaseLock( lock );
		}
		return sb;
	}


	
	private static void func1() {

		BPlusTree btree = new BPlusTree(80, 120, DiskMode.MEMORY_MAP);
		try {
			btree.createOpen (null, "a1.txt");
	
			long start = System.currentTimeMillis();
			
			int cnt = 100000;
			for (int i = 0; i < cnt; i += 2) {
				
				byte[] value = String.format("value_%d",i).getBytes();
				boolean isInsert = btree.insert(String.format("Aey_%d",i).getBytes(),value);
				if (!isInsert) {
				//	System.out.println("********************* 중복!! " + i);
					//System.exit(1);
				}
			}
			for (int i = 1; i < cnt; i += 2) {
				
				byte[] value = String.format("value_%d",i).getBytes();
				boolean isInsert = btree.insert(String.format("Aey_%d",i).getBytes(),value);
				if (!isInsert) {
				//	System.out.println("********************* 중복!! " + i);
					//System.exit(1);
				}
			}

			long end = System.currentTimeMillis();
			System.out.println( "실행 시간 : " + ( end - start )/1000.0 );

			start = System.currentTimeMillis();

			for (int i = cnt-1; i >= 0; --i) {

				ByteBuffer value = ByteBuffer.allocate( 120 );
				value.put( String.format("value_%d",i).getBytes() );
				value.clear();
				//
				byte[] res = btree.select(String.format("Aey_%d",i).getBytes());
				if( 0 == ByteBuffer.wrap(res).compareTo( value ) ) {
				//	System.out.println("성공:" + i + " " + new String(res));
				}
				else {
					System.out.println("실패!! " + i+ " " + new String(res) + " len:" + res.length);
					System.exit(1);					
				}
			}		
			
			end = System.currentTimeMillis();
			System.out.println( "실행 시간 : " + ( end - start )/1000.0 );

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void func2() {

		BPlusTree btree = new BPlusTree(80, 120, DiskMode.DISK);
		try {

			btree.createOpen (null, "a2.txt");

			int cnt = 30;
			for (int i = 0; i < cnt; i += 2) {
				
				byte[] value = String.format("value_%d",i).getBytes();
				boolean isInsert = btree.insert(String.format("Aey_%d",i).getBytes(),value);
				if (!isInsert) {
					System.out.println("********************* 중복!! " + i);
//					System.exit(1);
				}
			}
			
			for (int i = 1; i < 2; i += 2) {
				
				byte[] value = String.format("value_%d",i).getBytes();
				boolean isInsert = btree.insert(String.format("Aey_%d",i).getBytes(),value);
				if (!isInsert) {
					System.out.println("********************* 중복!! " + i);
//					System.exit(1);
				}
			}

			System.out.println(btree.display().toString());


	/*		
			btree.insert("015".getBytes(),"A".getBytes());
			btree.insert("069".getBytes(),"B".getBytes());
			btree.insert("110".getBytes(),"C".getBytes());
			btree.insert("090".getBytes(),"D".getBytes());
			btree.insert("020".getBytes(),"E".getBytes());
			btree.insert("120".getBytes(),"F".getBytes());
			btree.insert("040".getBytes(),"G".getBytes());
			btree.insert("125".getBytes(),"H".getBytes());
	*/		
	/*		
			btree.insert("K10".getBytes(),"A1".getBytes());
			btree.insert("K1".getBytes(),"A2".getBytes());
			System.out.println(btree.display().toString());
*/

//			byte[] res = btree.select(String.format("Aey_%d",1).getBytes());
//			System.out.println("res:" +  new String(res) + " len:" + res.length);


			
//btree.displayDump();
			
//			byte[] res = btree.select( "key_3".getBytes() );
//			System.out.println("res:" + new String(res));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void func3() {

		BPlusTree btree = new BPlusTree(80, 120, DiskMode.DISK);
		try {

			btree.createOpen (null, "a3.txt");

			int cnt = 30;
			for (int i = 0; i < cnt; i += 1) {
				
				byte[] value = String.format("value_%d",i).getBytes();
				boolean isInsert = btree.insert(String.format("key_%d",i).getBytes(),value);
				if (!isInsert) {
					System.out.println("********************* 중복!! " + i);
//					System.exit(1);
				}
			}
		//	System.out.println(btree.display().toString());
		//	System.exit(1);

			for (int i = cnt-1; i >= 0; i -= 1) {
				boolean isD = btree.delete( String.format("key_%d",i).getBytes() );
				if( !isD ) {
					System.out.println("FUCK "+ i);
					System.exit(1);
				}
				else {
					System.out.println("OK "+ i);
				}
			}
			
			byte[] res = btree.select( "key_3".getBytes() );
			System.out.println("res:" + new String(res));
			
			boolean isD = btree.delete( "k1ey_3".getBytes() );
			if( !isD ) {
				System.out.println("OK");
			}
			else {
				System.out.println("FUCK!");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	// public static void main(String[] args) {

	// 	func1();
	// }
	
}
