package hpr.util;

import java.util.ArrayList;
import java.util.List;


public class SlidingWindowQue<E> {
	
	private int maxCnt_;
    private List<Pair<String, E>> data_ = new ArrayList<Pair<String, E>>();
	
    public SlidingWindowQue(int maxCnt) {
    	maxCnt_ = maxCnt;
	}
    
	public synchronized boolean isEmpty() {
		return data_.isEmpty();
	}
	
	public synchronized int size() {
		return data_.size();
	}
	
	public synchronized boolean push( String key, E value ) {
		if( data_.size() >= maxCnt_ )
			return false;
		
		return data_.add( new Pair<String, E>(key, value));
	}
	
	public synchronized E popForKey( String key ) {
		Pair<String, E> data = null;
		for( int i = 0; i < data_.size(); ++i ) {
			data = data_.get(i);
			if( data.getKey().equals(key)) {
				data_.remove(i);
				break;
			}
		}

		if( null != data ) {
			return data.getValue();
		}
		else {
			return null;
		}
	}

	public synchronized E pop() {
		
		if( data_.isEmpty() )
			return null;
		
		Pair<String, E> data = data_.get(0);
		data_.remove(0);
		
		return data.getValue();
	}
}
