package tio

import scala.util.control.NonFatal
import java.time.Instant
import java.time.Duration
import tio.Clock.sleep
import scala.util.Random
import java.util.concurrent.TimeUnit

object TestEffect extends TIOApp:
  def run(args: Array[String]): TIO[Any] = for {
    _ <- putStrLn("hi")
    _ <- putStrLn("or not")
  } yield 0

object FailAndRecover extends TIOApp:
  def run(args: Array[String]) =
    (for {
      _ <- putStrLn("running first effect")
      _ <- TIO.fail(new RuntimeException)
      _ <- putStrLn("second effect - will not run")
    } yield ()).recover { case NonFatal(e) =>
      putStrLn(s"recovered from failure: ${e.getClass.getName}")
    }

object StackOflow extends TIOApp:
  def run(args: Array[String]): TIO[Any] =
    foreach(1 to 7000)(x => putStrLn(x))

object ClockTest extends TIOApp {
  override def run(args: Array[String]): TIO[Any] = for {
    _ <- putStrLn(s"Before sleep ${Instant.now()}, running on ${Thread.currentThread().getName()}")
    _ <- sleep(Duration.ofSeconds(5))
    _ <- putStrLn(s"After sleep ${Instant.now()}, running on ${Thread.currentThread().getName()}")
  } yield 0
}

object FiberTest extends TIOApp:
  override def run(args: Array[String]): TIO[Any] = for {
    _     <- putStrLn("Fiber step 1")
    fiber <- (sleep(Duration.ofSeconds(2)) *> putStrLn("Fiber step 2") *> TIO.succeed(0)).fork()
    _     <- putStrLn("Fiber step 3")
    _     <- fiber.join()
    _     <- putStrLn("last")
  } yield 1

object ForeachParExample extends TIOApp:
  val numbers = 1 to 10
  val random  = new Random()
  // sleep up to 1 second, and return the duration slept
  val sleepRandomTime = TIO
    .effect(Duration.ofMillis(random.nextInt(1000)))
    .flatMap(t => sleep(t) *> TIO.succeed(t))

  override def run(args: Array[String]): TIO[Any] =
    for {
      _ <- putStrLn(s"[${Instant.now}] foreach:")
      _ <- foreach(numbers)(i => putStrLn(i.toString))
      _ <- putStrLn(s"[${Instant.now}] foreachPar:")
      _ <- foreachPar(numbers)(i => sleepRandomTime.flatMap(t => putStrLn(s"$i after $t")))
      _ <- putStrLn(s"[${Instant.now}] foreachPar done")
    } yield ()
