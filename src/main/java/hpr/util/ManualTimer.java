package hpr.util;

import java.util.Date;

public class ManualTimer {
	
	private Date lastUpdateTime_;
	

	public boolean isTimeout(long miliseconds) {

		Date before = new Date();
		before.setTime(before.getTime() - miliseconds);

		if( null == lastUpdateTime_ || lastUpdateTime_.before(before)) {
			lastUpdateTime_ = new Date();
			return true;
		}

		return false;
	}
	
	

	// public static void main(String[] args) {
	// 	ManualTimer n = new ManualTimer();
	
	// 	while(true) {
	// 		boolean b = n.isTimeout(2000);
	// 		System.out.println("b:" + new Date() + b);
	// 		try {
	// 			Thread.sleep(400);
	// 		} catch (InterruptedException e) {
	// 			// TODO Auto-generated catch block
	// 			e.printStackTrace();
	// 		}
	// 	}
	// }
	

}
