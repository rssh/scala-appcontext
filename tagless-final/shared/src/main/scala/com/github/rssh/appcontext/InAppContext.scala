package com.github.rssh.appcontext



type InAppContext[Dependencies <: NonEmptyTuple] = [F[_]] =>> AppContextAsyncProviders[F,Dependencies]

object InAppContext {

  class FProvider[F[_]]:
    def apply[T](using p:AppContextAsyncProvider[F,T]): F[T] =
      p.get

  def get[F[_],T](using p: AppContextAsyncProvider[F,T]): F[T] =
      p.get

  def get1[F[_]](using p: AppContextAsyncProvider[F,?]): FProvider[F] =
    new FProvider[F]

}

extension(ac:AppContext.type)

  def asyncGet[F[_],T](using p: AppContextAsyncProvider[F,T]): F[T] =
        p.get



