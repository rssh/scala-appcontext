package com.github.rssh.appcontext


trait AppContextAsyncProvidersSearch[F[_],Dependencies<:NonEmptyTuple] {

   def getProvider[T,N<:Int](using TupleIndex.OfSubtype[Dependencies,T,N]): AppContextAsyncProvider[F,T]

   def get[T,N<:Int](using TupleIndex.OfSubtype[Dependencies,T,N]): F[T] =
        getProvider[T,N].get

}
