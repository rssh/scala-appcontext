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


/**
 * AppContextProviderLookup is a helper trait used by AppContext[T] to search for providers
 * with correct priority: AppContextProviders in enclosing scope takes precedence over
 * AppContextProvider[T] defined in T's companion object.
 *
 * This works because implicit search looks in the companion of the result type (T),
 * but AppContextProviderLookup[T]'s companion is AppContextProviderLookup, not T.
 * So givens defined here (fromProviders) have priority over T's companion.
 */
trait AppContextProviderLookup[T] {
  def get: T
}

trait AppContextProviderLookupLowPriority {
  /**
   * Low priority fallback: delegate to AppContextProvider[T].
   * Used when no AppContextProviders is in scope.
   */
  given fromProvider[T](using provider: AppContextProvider[T]): AppContextProviderLookup[T] with {
    def get: T = provider.get
  }
}

object AppContextProviderLookup extends AppContextProviderLookupLowPriority {

  /**
   * High priority: lookup from AppContextProviders in scope.
   * This given is in AppContextProviderLookup companion, so it's found before
   * any AppContextProvider[X] in X's companion.
   */
  given fromProviders[Xs <: NonEmptyTuple, X, N <: Int](
    using providers: AppContextProvidersSearch[Xs],
    idx: TupleIndex.OfSubtype[Xs, X, N]
  ): AppContextProviderLookup[X] with {
    def get: X = providers.getProvider[X, N].get
  }

}

