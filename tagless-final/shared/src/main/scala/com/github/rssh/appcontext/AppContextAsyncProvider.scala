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
