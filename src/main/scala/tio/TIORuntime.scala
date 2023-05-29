package tio

import scala.concurrent.duration.Duration
import scala.concurrent.Promise
import scala.concurrent.Await
import tio.TIO.*
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import javax.security.auth.callback.Callback
import java.util.concurrent.atomic.AtomicReference

trait Runtime:
  def unsafeRunSync[A](a: TIO[A], timeout: Duration): Try[A] =
    Await.ready(unsafeRunToFuture(a), timeout).value.get
  def unsafeRunAsync[A](a: TIO[A])(callback: Try[A] => Unit): Unit
  def unsafeRunToFuture[A](a: TIO[A]) =
    val p = Promise[A]()
    unsafeRunAsync(a)(p.tryComplete)
    p.future

object Runtime extends Runtime:
  private val executor = Executor.fixed(10, "TioRuntime")

  private class FiberRuntime(tio: TIO[Any]) extends Fiber[Any] {
    type Callbacks = Set[AsyncDoneCallback[Any]]
    private val joined  = AtomicReference[Callbacks](Set.empty)
    private val results = AtomicReference[Option[Try[Any]]](None)
    override def onDone(done: AsyncDoneCallback[Any]): FiberRuntime =
      joined.updateAndGet(_ + done)
      results.get().foreach(done)
      this

    private def fiberDone(res: Try[Any]) =
      results.set(Some(res))
      joined.get.foreach(_(res))

    def start(): Unit =
      eval(tio)(fiberDone)

    private def eval[A](tio: TIO[A])(onComplete: Try[Any] => Unit): Unit = executor.submit {
      tio match
        case Effect(a)                 => onComplete(Try(a()))
        case EffectAsync(taskCallback) => taskCallback(onComplete)
        case FlatMap(tio, body: (Any => TIO[Any])) =>
          eval(tio) {
            case Success(a) => eval(body(a))(onComplete)
            case Failure(f) => onComplete(Failure(f))
          }
        case Recover(tio, handler) =>
          eval(tio) {
            case Failure(exception) => eval(handler(exception))(onComplete)
            case Success(value)     => onComplete(Success(value))
          }
        case Fail(e) => onComplete(Failure(e))

        case Join(j) => j.onDone(onComplete)

        case Fork(tio) =>
          val fiber = new FiberRuntime(tio)
          fiber.start()
          onComplete(Success(fiber))
    }
  }

  override def unsafeRunAsync[A](a: TIO[A])(callback: Try[A] => Unit): Unit =
    new FiberRuntime(a)
      .onDone(callback.asInstanceOf[AsyncDoneCallback[Any]])
      .start()
