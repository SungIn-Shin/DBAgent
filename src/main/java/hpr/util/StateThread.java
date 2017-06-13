package hpr.util;

import java.util.concurrent.atomic.AtomicReference;


public abstract class StateThread implements Runnable {
	
	public enum ThreadState {STOPPED, STOPPING, RUNNING}

	private final AtomicReference<ThreadState> state = new AtomicReference<ThreadState>(ThreadState.STOPPED);
	
	private Thread thread_ = null;
	
	protected abstract void onStart ();
	protected abstract void onStop ();
	protected abstract boolean onRun ();

	public boolean isRunning() {
		return state.get() == ThreadState.RUNNING;
	}

	public boolean isStopped() {
		return state.get() == ThreadState.STOPPED;
	}
	
	public boolean start() {
    	boolean res = state.compareAndSet(ThreadState.STOPPED, ThreadState.RUNNING);

    	if( res ) {
    		thread_ = new Thread( this );
    		thread_.start();
    	}
    	
    	return res;
    }
	
	public boolean stop() {
    	return state.compareAndSet(ThreadState.RUNNING, ThreadState.STOPPING);
    }
    
	@Override
	public void run() {

		onStart();
		
		try {
			while (state.get() == ThreadState.RUNNING) { 
				if( !onRun() )
					break;
			}
		}
		finally {
			state.set(ThreadState.STOPPED);
			thread_ = null;

			onStop();
		}
	}
}
