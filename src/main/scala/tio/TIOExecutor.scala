package tio

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors.*

trait Executor:
  final def submit(task: => Unit) = submitRunnable(() => task)
  def submitRunnable(f: Runnable): Unit

object Executor:
  private val threadCount  = new AtomicInteger(0)
  private def nextThreadId = threadCount.incrementAndGet()

  def newDaemonThread(namePrefix: String): ThreadFactory = { thunk =>
    val t = new Thread(thunk, s"$namePrefix-$nextThreadId")
    t.setDaemon(true)
    t.setUncaughtExceptionHandler((_, e) => e.printStackTrace())
    t
  }

  def fixed(threads: Int, namePrefix: String): Executor = {
    val executor = newFixedThreadPool(threads, newDaemonThread(namePrefix))
    thunk => executor.submit(thunk)
  }
