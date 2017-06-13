package hpr.que.filedb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DiskPageMgr {
/*
	private static int HEAD_SIZE = 12;

	private void readHeader () throws IOException {
		pageSize_ 		= file_.getInt( 0 );
		pageIdxMax_ 	= file_.getInt( 4 );
		pageIdxFree_ 	= file_.getInt( 8 );
	}
	
	private void writeHeader () throws IOException {
		file_.putInt( 0, pageSize_ );
		file_.putInt( 4, pageIdxMax_ );
		file_.putInt( 8, pageIdxFree_ );
	}
*/	
	private int pageSize_;
	private int pageIdxMax_;
	private int pageIdxFree_;

	private PhysicalFile file_;
	
	public DiskPageMgr( PhysicalFile file,  int pageSize  ) {
		file_ = file;

		pageSize_	= pageSize;
		pageIdxMax_ = 0;
		pageIdxFree_= -1;
	}

	public int getPageIdxMax () {
		return pageIdxMax_;
	}
	
	public int getPageIdxFree () {
		return pageIdxFree_;
	}

	public void setPageIdxMax( int pageIdxMax ) {
		pageIdxMax_ = pageIdxMax;
	}
	
	public void setPageIdxFree( int pageIdxFree ) {
		pageIdxFree_ = pageIdxFree;
	}	
	
	public int alloc () throws IOException {

		int pageIdx = -1;

		if (-1 == pageIdxFree_) {
			pageIdxMax_ += 1;
			pageIdx = pageIdxMax_;
		}
		else {
			pageIdx = pageIdxFree_;
			pageIdxFree_ = file_.getInt( pageIdx * pageSize_ );
		}

		if (0 > pageIdx) {
			throw new RuntimeException ("pageIdx should not be under 1: " + pageIdx);
		}

		return pageIdx;
	}
	
	public void dealloc (int pageIdx) throws IOException {
		if (1 > pageIdx) {
			throw new RuntimeException ("pageIdx should not be under 1: " + pageIdx);
		}
		
		file_.putInt( pageIdx * pageSize_, pageIdxFree_ );

		pageIdxFree_ = pageIdx;
	}

	public void writePage( int pageIdx, int position, byte[] src, int offset, int length ) throws IOException {
		if( length > (pageSize_ - position)) {
			throw new RuntimeException ("Overflow pageSize (" + length + " > " + (pageSize_ - position) + " )");
		}
		
		if( length > src.length) {
			throw new RuntimeException ("Overflow length (" + length + " > " + src.length + " )");
		}
		
		file_.put( pageIdx * pageSize_ + position, src, offset, length);
	}
	

	public void readPage( int pageIdx, int position, byte[] dst, int length ) throws IOException {
		if( length > (pageSize_ - position)) {
			throw new RuntimeException ("Overflow pageSize (" + length + " > " + (pageSize_ - position) + " )");
		}
		
		if( length > dst.length) {
			throw new RuntimeException ("Overflow length (" + length + " > " + dst.length + " )");
		}
		
		file_.get( pageIdx * pageSize_ + position, dst, length);
	}
	

	
// 	public static void main(String[] args) {

		
// 		boolean isR = false;
// //		isR = true;
		
// 		MemoryMapedFile mapFile = new MemoryMapedFile();
		
// 		try {
// 			mapFile.createOpen("diskpage_test7.dat");
// 			DiskPageMgr pageMgr = new DiskPageMgr(mapFile, 1024);

// 			if (isR) {
				
// 				byte[] readB = new byte[1024];
// 				pageMgr.readPage(0, 0, readB, readB.length);
// 				ByteBuffer rb = ByteBuffer.wrap(readB);
				
// 				int max = rb.getInt();
// 				int free = rb.getInt();
	
// 				pageMgr.setPageIdxMax(max);
// 				pageMgr.setPageIdxFree(free);
				
// 				System.out.println("max:" + max);
// 				System.out.println("free:" + free);
// 			}

// 			ArrayList<Integer> list = new ArrayList<Integer>();
			
// 			for( int i = 0; i < 100; ++i) {
// 				int page = pageMgr.alloc();
// 				System.out.println("alloc page:" + i + "->" + page);

// 				list.add(page);
// 			}
			
// 			for(int i: list) {
// 				System.out.println("dealloc page:" + i);
// 				pageMgr.dealloc(i);
// 			}
			
			
			
// 			//pageMgr.dealloc(1);
			
			
			
// 			int page2 = pageMgr.alloc();
// 			System.out.println("alloc page2:" + page2);
			
// 			System.out.println("dealloc page1:" + page1);
// 			pageMgr.dealloc(page1);
// 			int page3 = pageMgr.alloc();
// 			System.out.println("alloc page3:" + page3);
// 			System.out.println("dealloc page2:" + page2);
// 			pageMgr.dealloc(page2);
// 			System.out.println("dealloc page3:" + page3);
// 			pageMgr.dealloc(page3);
			
// 			int page4 = pageMgr.alloc();
// 			System.out.println("alloc page4:" + page4);
// 			int page5 = pageMgr.alloc();
// 			System.out.println("alloc page5:" + page5);
// 			int page6 = pageMgr.alloc();
// 			System.out.println("alloc page6:" + page6);
			
// 			byte[] d = new byte[8];
// 			ByteBuffer b = ByteBuffer.wrap(d);
			
// 			int max1= pageMgr.getPageIdxMax();
// 			int free1= pageMgr.getPageIdxFree();
			
// 			b.putInt(max1);
// 			b.putInt(free1);
			
// 			System.out.println("max1:" + max1);
// 			System.out.println("free1:" + free1);
			
// 			pageMgr.writePage(0, 0, b.array(), 0, b.array().length);
// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
// 	}
}
