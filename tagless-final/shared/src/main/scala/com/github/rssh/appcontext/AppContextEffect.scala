package com.github.rssh.appcontext



type AppContextEffect[Dependencies <: NonEmptyTuple] = [F[_]] =>> AppContextAsyncProviders[F,Dependencies]

object AppContextEffect {

  def get[F[_],T](using p: AppContextAsyncProvider[F,T]): F[T] =
      p.get

}

extension(ac:AppContext.type)

  def async[F[_],T](using p: AppContextAsyncProvider[F,T]): F[T] =
        p.get



