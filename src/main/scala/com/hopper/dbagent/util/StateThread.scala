package com.hopper.dbagent.util

trait StateThread extends Runnable {

  @volatile protected var keepGoing: Boolean = false
  private var thread: Thread = _

  def start: Unit = synchronized {
    if (!keepGoing) {
      keepGoing = true
      thread = new Thread(this)
      thread.start()
    }
  }

  def stop: Unit = {
    if (keepGoing && null != thread) synchronized {
      keepGoing = false
      thread.interrupt()
    }
  }
}
