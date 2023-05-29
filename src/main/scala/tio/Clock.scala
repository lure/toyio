package tio

import java.time.Duration
import TIO.effectAsync
import java.util.Timer
import java.util.TimerTask
import scala.util.Success
import tio.Clock.sleep
import java.util.concurrent.TimeUnit
import java.time.Instant

object Clock:
  val timer = new Timer("Tio-timer", true)
  def sleep(duration: Duration): TIO[Unit] =
    effectAsync { onComplete =>
      timer.schedule(
        new TimerTask() {
          override def run(): Unit = onComplete(Success(()))
        },
        duration.toMillis()
      )
    }
