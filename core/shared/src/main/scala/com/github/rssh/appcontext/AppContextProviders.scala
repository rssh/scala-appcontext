package com.github.rssh.appcontext


import scala.quoted.*

trait AppContextProvidersSearch[Xs<:NonEmptyTuple] {

  def getProvider[T,N<:Int](using TupleIndex.OfSubtype[Xs,T,N]): AppContextProvider[T]

  def get[T, N<:Int](using TupleIndex.OfSubtype[Xs,T, N]): T = getProvider[T,N].get

}

trait AppContextProviders[Xs <: NonEmptyTuple] extends AppContextProvidersSearch[Xs] {

  override def getProvider[T,N<:Int](using TupleIndex.OfSubtype[Xs,T,N]): AppContextProvider[T]

  // for unsafe access from macro
  def unsafeGetProvider[T](i:Int): AppContextProvider[T]

  def unsafeGet[T](i:Int): T = unsafeGetProvider[T](i).get

}


object AppContextProviders {

  /**
   * Accept a tuple of services and create an AppContextProviders for then.
   * ```
   *   val providers = AppContextProviders.of((service1, Service2))
   *   val depended = new Dependend(using providers)
   * ```
   * @param values
   * @tparam T
   * @return
   */
  def of[T <: NonEmptyTuple](values: T): AppContextProviders[T] = {
    val arr = values.productIterator.map(AppContextProvider.of(_):AppContextProvider[?]).toArray
    DefaultAppContextProviders[T](arr)
  }

  protected class DefaultAppContextProviders[Xs <: NonEmptyTuple](array: Array[AppContextProvider[?]]) extends AppContextProviders[Xs] {
    def getProvider[T,N<:Int](using TupleIndex.OfSubtype[Xs,T,N]): AppContextProvider[T] =
      array(summon[TupleIndex.OfSubtype[Xs,T,N]].index).asInstanceOf[AppContextProvider[T]]
    def unsafeGetProvider[T](i:Int): AppContextProvider[T] =
      array(i).asInstanceOf[AppContextProvider[T]]  
  }

  inline given generate[T<:NonEmptyTuple]: AppContextProviders[T] = ${ generateImpl[T] }

  private def generateImpl[T<: NonEmptyTuple:Type](using Quotes): Expr[AppContextProviders[T]] = {
    import quotes.reflect.*
    val tupleType = TypeRepr.of[T]
    val tupleTypes = TupleMacroses.extractTupleTypes(tupleType)
    val listResolved = tupleTypes.map{ t =>
      val tProvider = TypeRepr.of[AppContextProvider].appliedTo(t)
      Implicits.search(tProvider) match
        case failure:ImplicitSearchFailure =>
          report.throwError(s"Cannot find ${tProvider.show}: ${failure.explanation}")
        case success:ImplicitSearchSuccess =>
          success.tree.asExprOf[AppContextProvider[?]]
    }
    val retval = '{
      new DefaultAppContextProviders[T](
        ${Expr.ofSeq(listResolved)}.toArray
      )
    }
    retval
  }

}


