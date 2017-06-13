package hpr.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class AtTimer {

	public enum HowOften {MINUTELY, HOURLY, DAILY, MONTHLY}
	
	private Timer timer_ = new Timer(true);	// as a deamon thraed

	private class Task {
		
		private Runnable whatToDo_;
		private HowOften howOften_;
		private int delaySec_;
		
		public Task( Runnable whatToDo, HowOften howOften, int delaySec) {
			whatToDo_ = whatToDo;
			howOften_ = howOften;
			delaySec_ = delaySec;
			
			registSched();
		}
		private void registSched() {
			
			timer_.schedule(new TimerTask() {

				@Override
				public void run() {
					whatToDo_.run();
					registSched();
				}
				
			}, next() );
		}
		
		private Date next() {
			
			Calendar date = Calendar.getInstance();

			if( howOften_ == HowOften.MINUTELY) {
				
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);

				date.add(Calendar.MINUTE, 1); // 다음분
			}
			else if( howOften_ == HowOften.HOURLY) {
				
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);

				date.add(Calendar.HOUR, 1); // 다음시간
			}
			else if( howOften_ == HowOften.DAILY) {
				
				date.set(Calendar.HOUR_OF_DAY, 0);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);

				date.add(Calendar.DATE, 1); // 다음날
			}
			else {
				date.set(Calendar.DAY_OF_MONTH, 1);
				date.set(Calendar.HOUR_OF_DAY, 0);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);

				date.add(Calendar.MONTH, 1); // 다음달
			}
			
			// 공통 딜레이
			date.add(Calendar.SECOND, delaySec_);
			return date.getTime();
		}
	}
	
	private ArrayList<Task> tasks_= new ArrayList<Task>();
	
	public void add( Runnable runnable, HowOften howOften, int delaySec ) {
		tasks_.add( new Task(runnable, howOften, delaySec) );
	}
	
	public void cancel() {
		timer_.cancel();
		timer_.purge();
		tasks_.clear();
	}

	
		                                                                                                                                                                                                                                                                                                                                                                                                  
	// public static void main(String[] args) {

	// 	AtTimer e = new AtTimer();

	// 	e.add(new Runnable() {

	// 		@Override
	// 		public void run() {
	// 			System.out.println("A:" + new Date());
	// 			try {
	// 				Thread.sleep(1000*3);
	// 			} catch (InterruptedException e) {}

	// 		}
	// 	}, HowOften.MINUTELY, 4);

	// 	e.add(new Runnable() {
	// 		@Override
	// 		public void run() {
	// 			System.out.println("B:" + new Date());
	// 			try {
	// 				Thread.sleep(1000*3);
	// 			} catch (InterruptedException e) {}
	// 		}
	// 	}, HowOften.HOURLY, 2);
	// 	e.add(new Runnable() {

	// 		@Override
	// 		public void run() {
	// 			System.out.println("C:" + new Date());
	// 			try {
	// 				Thread.sleep(1000*3);
	// 			} catch (InterruptedException e) {}
	// 		}
	// 	}, HowOften.DAILY, 0);

	// 	e.add(new Runnable() {

	// 		@Override
	// 		public void run() {
	// 			System.out.println("D:" + new Date());
	// 			try {
	// 				Thread.sleep(1000*3);
	// 			} catch (InterruptedException e) {}
	// 		}
	// 	}, HowOften.MONTHLY, 0);

	// 	try {
	// 		Thread.sleep(1000 * 60 * 60 * 24);
	// 	} catch (InterruptedException e1) {
	// 		// TODO Auto-generated catch block
	// 		e1.printStackTrace();
	// 	}




	// }
}