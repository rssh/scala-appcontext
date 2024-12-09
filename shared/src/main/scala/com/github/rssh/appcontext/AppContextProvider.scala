package com.github.rssh.appcontext


import scala.compiletime.*
import scala.quoted.*

trait AppContextProvider[T] {
  def get: T

}


object AppContextProvider {

  def of[T](value: T): AppContextProvider[T] = new AppContextProvider[T]:
    override def get: T = value

  given ofGiven[T](using T): AppContextProvider[T] with {
    def get: T = summon[T]
  }

  given fromProviders[Xs <: NonEmptyTuple, X, N <: Int](using AppContextProvidersSearch[Xs], TupleIndex.OfSubtype[Xs, X, N]): AppContextProvider[X] =
    summon[AppContextProvidersSearch[Xs]].getProvider[X, N]

}

