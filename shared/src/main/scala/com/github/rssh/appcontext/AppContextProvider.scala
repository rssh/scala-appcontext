package com.github.rssh.appcontext


import scala.compiletime.*
import scala.quoted.*


/**
 * AppContextProvider is a type class that provides a value of type T
 *
 * When we have AppContextProvider for a type T, then we can resolve T
 * for dependency injection, using AppContext[T] syntax.
 * <code>
 *   AppContext[T]
 * </code>
 */
trait AppContextProvider[T] {
  def get: T
}


object AppContextProvider {

  /**
   * Create AppContextProvider for a value of type T
   *
   * @param value the value of type T
   * @return AppContextProvider for the value
   */
  def of[T](value: T): AppContextProvider[T] = new AppContextProvider[T]:
    override def get: T = value

  /**
   * Create default AppContextProvider for a implicit value of type T.
   */
  given ofGiven[T](using T): AppContextProvider[T] with {
    def get: T = summon[T]
  }

  /**
   * Create AppContextProvider for T from a set of AppContextProviders[Xs], where Xs is a tuple of types containing T.
   */
  given fromProviders[Xs <: NonEmptyTuple, X, N <: Int](using AppContextProvidersSearch[Xs], TupleIndex.OfSubtype[Xs, X, N]): AppContextProvider[X] =
    summon[AppContextProvidersSearch[Xs]].getProvider[X, N]

}

