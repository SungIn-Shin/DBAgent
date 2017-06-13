package hpr.net.http;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class SessionMap {
	
	private Map<String, Date> sessionMap_ = new HashMap<String, Date>();
	
	private Date lastCleaningTime_ = null;
	
	private long timeoutSec_;
	
	public SessionMap( long timeoutSec ) {
		timeoutSec_ = timeoutSec;
	}
	
	public boolean get(String key) {
		
		removeTimeout(1000 * timeoutSec_);
		
		if( sessionMap_.get(key) == null) {
			return false;
		}
		
		put(key);
		
		return true;
	}
	
	public void remove(String key) {
		sessionMap_.remove(key);
	}
	
	public void put(String key) {
		sessionMap_.put(key, new Date());
	}
	
	public void removeTimeout( long howOldSec ) {
		
		if( !isTimeouted(1000) ) {
			return;
		}
		
		Date beforeDate = new Date();
		beforeDate.setTime(beforeDate.getTime() - howOldSec);
		
		for( Iterator<Map.Entry<String, Date>> it = sessionMap_.entrySet().iterator(); it.hasNext(); ) {
			
			Map.Entry<String, Date> entry = it.next();
			
			
			if( null == entry.getValue() || beforeDate.after(entry.getValue())) {
				it.remove();
			}
		}
	}

	private boolean isTimeouted( long howOldSec ) {

		Date beforeDate = new Date();
		beforeDate.setTime(beforeDate.getTime() - howOldSec);

		if( null == lastCleaningTime_ || lastCleaningTime_.before(beforeDate)) {
			lastCleaningTime_ = new Date();
			return true;
		}
		else {
			return false;
		}

	}
	

	// public static void main(String[] args) {
	// 	SessionMap s = new SessionMap(60);
		
	// 	s.put("A");
	// 	s.put("B");
	// 	try {
	// 		Thread.sleep(1000);
	// 	} catch (InterruptedException e) {}
	// 	s.put("C");
	// 	try {
	// 		Thread.sleep(1000);
	// 	} catch (InterruptedException e) {}
	// 	s.put("B");
	// 	s.removeTimeout(1000);
		
	// 	try {
	// 		Thread.sleep(1000);
	// 	} catch (InterruptedException e) {}
	// 	s.put("A");
		
	// 	s.removeTimeout(1000);
	// 	s.removeTimeout(1000);
	// }

}
