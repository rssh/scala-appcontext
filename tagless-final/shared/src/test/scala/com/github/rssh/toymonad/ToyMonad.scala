package com.github.rssh.toymonad


import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*
import scala.util.control.NonFatal
import cps.*
import cps.monads.CpsIdentityMonad.flatMapTry

import scala.annotation.tailrec


sealed trait ToyMonad[+A]

object ToyMonad {
 
  case class Pure[A](a: ()=>A) extends ToyMonad[A] {
    type TA = A
    type R = A
  }
  case class FlatMapTry[A,B](m: ToyMonad[A], f: Try[A] => ToyMonad[B]) extends ToyMonad[B] {
    type TA = A
    type TB = B
    type R = B
  }
  case class FutureWait[A](f: Future[A]) extends ToyMonad[A] {
    type TA = A
    type R = A
  }
  
  type Reader[R,A] = ToyMonadReader.Reader[R,A]

  object CpsEffectToyMonad extends CpsEffectMonad[ToyMonad] with CpsAsyncMonadInstanceContext[ToyMonad] {

    def pure[A](a: A): ToyMonad[A] = Pure(()=>a)

    def map[A,B](fa: ToyMonad[A])(f: A => B): ToyMonad[B] =
      flatMap(fa){ (a:A) => pure(f(a)) }

    def flatMap[A,B](fa: ToyMonad[A])(f: A => ToyMonad[B]): ToyMonad[B] =
      FlatMapTry( fa, {
        case Success(a) => f(a)
        case Failure(e) => error(e)
      })

    override def error[A](e: Throwable): ToyMonad[A] =
      FutureWait(Future.failed(e))

    override def flatMapTry[A, B](fa: ToyMonad[A])(f: Try[A] => ToyMonad[B]): ToyMonad[B] = {
      FlatMapTry(fa, f)
    }

    override def adoptCallbackStyle[A](source: (Try[A] => Unit) => Unit): ToyMonad[A] = {
      val p = Promise[A]
      source( (ta:Try[A]) => p.complete(ta) )
      FutureWait(p.future)
    }


  }

  given CpsEffectMonad[ToyMonad] = CpsEffectToyMonad

  def error[A](e: Throwable): ToyMonad[A] = CpsEffectToyMonad.error(e)
  def pure[A](a: A): ToyMonad[A] = CpsEffectToyMonad.pure(a)

  def fromTry[A](ta: Try[A]): ToyMonad[A] =
    ta match
      case Success(a) => Pure(()=>a)
      case Failure(e) => FutureWait(Future.failed(e))

  def run[A](fa: ToyMonad[A]): Future[A] = {

    def innerLoop1[A](fa: ToyMonad[A]): Future[A] =
      innerLoop(fa)
    
    @tailrec
    def innerLoop[A](fa: ToyMonad[A]): Future[A] = {
      fa match {
        case Pure(a) => Future.fromTry(Try(a()))
        case fma@FlatMapTry(fa,f) =>
          fa match
            case Pure(a) =>
              Try(f(Try(a().asInstanceOf[fma.TA]))) match
                case Success(fb) => innerLoop(fb)
                case Failure(ex) => Future.failed(ex)
            case fmz@FlatMapTry(fz, g) =>
              innerLoop(
                FlatMapTry(fz, (ta:Try[fmz.TA]) => 
                  FlatMapTry(g(ta).asInstanceOf[ToyMonad[fma.TA]], f)
                )
              )
            case FutureWait(future) =>
              future.transformWith{ ta =>
                val tb = 
                  try 
                    f(ta.asInstanceOf[Try[fma.TA]])
                  catch
                    case NonFatal(ex) => CpsEffectToyMonad.error(ex)
                innerLoop1(tb)
              }
        case FutureWait(f) =>
          f.transformWith(ta => innerLoop1(fromTry(ta)))
      }
    }
    
    innerLoop(fa)
    
  }


}