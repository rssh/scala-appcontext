package com.github.rssh.toymonad

import cps.*

import scala.util.Try

object ToyMonadReader  {

   opaque type Reader[R, A] = R => ToyMonad[A]
   
   class CpsToyMonadReaderEffectMonad[R] extends  CpsEffectMonad[[A]=>> Reader[R,A]] 
                                               with CpsAsyncMonadInstanceContext[[A]=>>Reader[R,A]]  {
     
      def pure[A](a: A): Reader[R,A] = (r:R) => ToyMonad.Pure(()=>a)
      
      def map[A,B](fa: Reader[R,A])(f: A => B): Reader[R,B] = 
         (r:R) => ToyMonad.CpsEffectToyMonad.map(fa(r))(f)
      
      def flatMap[A,B](fa: Reader[R,A])(f: A => Reader[R,B]): Reader[R,B] = 
         (r:R) => ToyMonad.CpsEffectToyMonad.flatMap(fa(r)){ a => f(a)(r) }
     
      def error[A](e: Throwable): Reader[R,A] = (r:R) => ToyMonad.CpsEffectToyMonad.error(e)
     
      def adoptCallbackStyle[A](source: (Try[A] => Unit) => Unit): Reader[R,A] = 
         (r:R) => ToyMonad.CpsEffectToyMonad.adoptCallbackStyle(source)
         
      def flatMapTry[A, B](fa: Reader[R,A])(f: Try[A] => Reader[R,B]): Reader[R,B] =
          (r:R) => ToyMonad.CpsEffectToyMonad.flatMapTry(fa(r)){ ta => f(ta)(r) }  
         
   }

   given [R]: CpsEffectMonad[[A]=>> Reader[R,A]] = CpsToyMonadReaderEffectMonad[R]
  
}
