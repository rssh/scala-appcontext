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
    val tupleTypes = extractTupleTypes(tupleType)
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


  inline def checkAllAreNeeded[Xs<:NonEmptyTuple](using p: AppContextProviders[Xs]): Boolean =
    ${ checkAllAreNeededImpl[Xs]('p) }

  private def checkAllAreNeededImpl[T<:NonEmptyTuple:Type](p: Expr[AppContextProviders[T]])(using Quotes): Expr[Boolean] = {
    import quotes.reflect.*
    val tupleType = TypeRepr.of[T]
    val tupleTypes = extractTupleTypes(tupleType)
    val fromProvidersSymbol = Symbol.requiredMethod("com.github.rssh.appcontext.AppContextProvider.fromProviders")
    var retval = true
    for{ t <- tupleTypes } {
      val tProvider = TypeRepr.of[AppContextProvider].appliedTo(t)
      Implicits.search(tProvider) match
        case failure: ImplicitSearchFailure =>
          // impossible, it will be success.
          report.error(s"Cannot find ${tProvider.show}: ${failure.explanation}")
        case success: ImplicitSearchSuccess =>
          // if this is 'this ?
          success.tree match
            case Apply(TypeApply(fun, targs), args@List(p1,ti)) if (fun.symbol == fromProvidersSymbol) =>
                if !(p1.tpe =:= p.asTerm.tpe) then
                  report.error(s"Provider for ${t.show} in ${p.show} is not necessory: ${success.tree.show} provider will be used")
                  retval = false
            case _ =>
              report.error(s"Provider for ${t.show} is not needed in ${p.show}, it will be resolved as: ${success.tree.show}")
              retval = false
    }
    Expr(retval)
  }

  private def extractTupleTypes(using Quotes)(t: quotes.reflect.TypeRepr): List[quotes.reflect.TypeRepr] = {
    import quotes.reflect.*
    t match
      case AppliedType(tf, args) if (defn.isTupleClass(tf.typeSymbol)) =>
        args
      case AppliedType(tf, args) if (tf.typeSymbol == Symbol.requiredClass("scala.*:")) =>
        args.head :: extractTupleTypes(args.tail.head)
      case tf if tf <:< TypeRepr.of[EmptyTuple] => List.empty
      case _ =>
        report.throwError(s"Type ${t.show} is not a tuple")
  }


}


