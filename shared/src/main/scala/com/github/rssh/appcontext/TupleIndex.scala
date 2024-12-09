package com.github.rssh.appcontext

import scala.compiletime.constValue
import scala.compiletime.ops.int.S
import scala.quoted.*


object TupleIndex {

  opaque type OfSubtype[Xs <: Tuple, T, N<:Int] = N

  extension [Xs <: Tuple, T, N<:Int](idx: TupleIndex.OfSubtype[Xs, T, N])
    def index: Int = idx

  inline given zeroOfSubtype[XsH, XsT <:Tuple, T<:XsH]: OfSubtype[XsH *: XsT, T, 0] = 0

  inline given nextOfSubtype[XsH, XsT <:NonEmptyTuple, T, N <: Int](using idx: OfSubtype[XsT, T, N]): OfSubtype[XsH *: XsT, T, S[N]] =
    constValue[S[N]]
  
}

