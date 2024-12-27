package com.github.rssh.appcontext

import com.github.rssh.appcontext.util.AppContextPure
import cps.*

trait AppContextAsyncProvider[F[_],T] {

  def get: F[T]

}

trait AppContextAsyncProviderLowLevel{


}


object AppContextAsyncProvider extends AppContextAsyncProviderLowLevel {


  given fromAsyncProvidersSearch[F[_], T, Xs <: NonEmptyTuple, N <: Int](
                                                            using AppContextAsyncProvidersSearch[F, Xs],
                                                            TupleIndex.OfSubtype[Xs, T, N]): AppContextAsyncProvider[F, T] =
    summon[AppContextAsyncProvidersSearch[F, Xs]].getProvider[T, N]


  /*
  given fromMonadAccess[F[_]:AppContextEffect[Xs], T, Xs <: NonEmptyTuple, N <: Int](
                                                            using TupleIndex.OfSubtype[Xs, T, N]): AppContextAsyncProvider[F, T] =
    new AppContextAsyncProvider[F, T] {
      def get: F[T] = AppContextEffect.get[F, T]
    }

   */

  given fromSync[F[_] : AppContextPure, T](using syncProvider: AppContextProvider[T]): AppContextAsyncProvider[F, T] with
    def get: F[T] = summon[AppContextPure[F]].pure(syncProvider.get)


}
