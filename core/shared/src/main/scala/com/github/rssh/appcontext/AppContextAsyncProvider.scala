package com.github.rssh.appcontext

import com.github.rssh.appcontext.util.AppContextPure
import cps.*


trait AppContextAsyncProvider[F[_],T] {

  def get: F[T]

}

trait AppContextAsyncProviderLowLevelImplicits{

  given fromSync[F[_] : AppContextPure, T](using syncProvider: AppContextProvider[T]): AppContextAsyncProvider[F, T] with
    def get: F[T] = summon[AppContextPure[F]].pure(syncProvider.get)


}

object AppContextAsyncProvider extends AppContextAsyncProviderLowLevelImplicits {

  def apply[F[_],T](using p: AppContextAsyncProvider[F,T]): AppContextAsyncProvider[F,T] =
    p

  def get[F[_],T](using p: AppContextAsyncProvider[F,T]): F[T] =
    p.get

  def of[F[_],T](value: F[T]): AppContextAsyncProvider[F,T] =
    new AppContextAsyncProvider[F,T] {
      def get: F[T] = value
    }

  /**
   * Create default AppContextProvider for a implicit value of type T.
   * Note, that name is used in macros (i.e. should not be changed)
   */
  given fromProviders[F[_], T, Xs <: NonEmptyTuple, N <: Int](
                                                            using AppContextAsyncProvidersSearch[F, Xs],
                                                            TupleIndex.OfSubtype[Xs, T, N]): AppContextAsyncProvider[F, T] =
    summon[AppContextAsyncProvidersSearch[F, Xs]].getProvider[T, N]


}


/**
 * AppContextAsyncProviderLookup is a helper trait used by InAppContext to search for async providers
 * with correct priority:
 * 1. AppContextAsyncProviders in enclosing scope (highest)
 * 2. Local AppContextProvider (sync) converted via fromSyncProvider
 * 3. AppContextAsyncProvider[F, T] defined in T's companion object (lowest)
 *
 * This works because implicit search looks in the companion of the result type (T),
 * but AppContextAsyncProviderLookup[F, T]'s companion is AppContextAsyncProviderLookup, not T.
 * So givens defined here have priority over T's companion.
 */
trait AppContextAsyncProviderLookup[F[_], T] {
  def get: F[T]
}

trait AppContextAsyncProviderLookupLowPriority {
  /**
   * Lowest priority fallback: delegate to AppContextAsyncProvider[F, T].
   * This picks up companion-defined async providers.
   */
  given fromAsyncProvider[F[_], T](using provider: AppContextAsyncProvider[F, T]): AppContextAsyncProviderLookup[F, T] with {
    def get: F[T] = provider.get
  }
}

trait AppContextAsyncProviderLookupMidPriority extends AppContextAsyncProviderLookupLowPriority {
  /**
   * Mid priority: convert sync AppContextProvider to async.
   * Local sync providers win over companion-defined async providers.
   */
  given fromSyncProvider[F[_]: AppContextPure, T](using syncProvider: AppContextProvider[T]): AppContextAsyncProviderLookup[F, T] with {
    def get: F[T] = summon[AppContextPure[F]].pure(syncProvider.get)
  }
}

object AppContextAsyncProviderLookup extends AppContextAsyncProviderLookupMidPriority {

  /**
   * Highest priority: lookup from AppContextAsyncProviders in scope.
   * This given is in AppContextAsyncProviderLookup companion, so it's found before
   * any AppContextAsyncProvider[F, X] in X's companion.
   */
  given fromProviders[F[_], Xs <: NonEmptyTuple, X, N <: Int](
    using providers: AppContextAsyncProvidersSearch[F, Xs],
    idx: TupleIndex.OfSubtype[Xs, X, N]
  ): AppContextAsyncProviderLookup[F, X] with {
    def get: F[X] = providers.getProvider[X, N].get
  }

}
