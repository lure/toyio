package tio

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import tio.Runtime

trait TIOApp:
  def run(args: Array[String]): TIO[Any]
  def main(args: Array[String]): Unit = Runtime.unsafeRunSync(run(args), Duration(20, TimeUnit.SECONDS)).get
