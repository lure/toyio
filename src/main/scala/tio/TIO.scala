package tio

import TIO.Effect
import TIO.FlatMap
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import TIO.Recover
import TIO.{Fail, EffectAsync}
import scala.util.control.NonFatal
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import java.util.concurrent.TimeUnit

sealed trait TIO[+A]:
  def flatMap[B](body: A => TIO[B]): TIO[B]      = TIO.FlatMap(this, body)
  def map[B](body: A => B): TIO[B]               = flatMap(a => TIO.succeed(body(a)))
  def *>[B](that: TIO[B]): TIO[B]                = flatMap(_ => that)
  def recover[B >: A](func: Throwable => TIO[B]) = TIO.Recover(this, func)
  def fork(): TIO[Fiber[A]]                      = TIO.Fork(this)

trait Fiber[+A]:
  def join(): TIO[A] = TIO.Join(this)
  private[tio] def onDone(done: TIO.AsyncDoneCallback[A]): Fiber[A]

object TIO:
  type AsyncDoneCallback[A] = Try[A] => Unit
  type AsyncTask[A]         = AsyncDoneCallback[A] => Unit

  case class Effect[+A](a: () => A)                                extends TIO[A]
  case class FlatMap[A, B](tio: TIO[A], body: A => TIO[B])         extends TIO[B]
  case class Recover[A](tio: TIO[A], handler: Throwable => TIO[A]) extends TIO[A]
  case class Fail[A](e: Throwable)                                 extends TIO[A]
  case class EffectAsync[+A](task: AsyncTask[A])                   extends TIO[A]
  case class Join[A](a: Fiber[A])                                  extends TIO[A]
  case class Fork[A](a: TIO[A])                                    extends TIO[Fiber[A]]

  def effect[A](a: => A): TIO[A]         = Effect(() => a)
  def succeed[A](a: A): TIO[A]           = Effect(() => a)
  def fail[A](a: Throwable): TIO[A]      = Fail(a)
  def effectAsync[A](task: AsyncTask[A]) = EffectAsync(task)
