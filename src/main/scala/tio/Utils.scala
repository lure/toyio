package tio

def foreach[A, B](xs: Iterable[A])(func: A => TIO[B]): TIO[Iterable[B]] =
  xs.foldLeft(TIO.succeed(Vector.empty[B])) { (acc, x) =>
    for {
      seq <- acc
      v   <- func(x)
    } yield seq :+ v
  }

def foreachPar[A, B](xs: Iterable[A])(func: A => TIO[B]): TIO[Iterable[B]] =
  foreach(xs)(func(_).fork()).flatMap(fibers => foreach(fibers)(_.join()))

def putStrLn(msg: => Any) = TIO.effect(println(msg))
