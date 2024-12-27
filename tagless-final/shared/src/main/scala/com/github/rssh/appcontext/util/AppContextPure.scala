package com.github.rssh.appcontext.util

import cps.*

trait AppContextPure[F[_]] {

  def pure[A](a: =>A): F[A]

}

object AppContextPure {

  given [F[_]:CpsMonad]: AppContextPure[F] with {

    def pure[A](a: =>A): F[A] =
      summon[CpsMonad[F]].wrap(a)
  }

}
