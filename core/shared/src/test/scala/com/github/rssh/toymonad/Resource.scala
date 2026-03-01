package com.github.rssh.toymonad

trait Resource[F[_],T] {
  def use[R](f: T => F[R]): F[R]
}
