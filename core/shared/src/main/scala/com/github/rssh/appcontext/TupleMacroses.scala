package com.github.rssh.appcontext

import scala.quoted.*

object TupleMacroses {


  def extractTupleTypes(using Quotes)(t: quotes.reflect.TypeRepr): List[quotes.reflect.TypeRepr] = {
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

  def checkAllAreNeeded[P[_]:Type, Ps[_<:NonEmptyTuple]:Type, Xs <: NonEmptyTuple : Type](using Quotes)(p: Expr[Ps[Xs]]): Expr[Boolean] = {
    import quotes.reflect.*
    val tupleType = TypeRepr.of[Xs]
    val tupleTypes = TupleMacroses.extractTupleTypes(tupleType)
    //val fromProvidersSymbol =  Symbol.requiredMethod("com.github.rssh.appcontext.AppContextProvider.fromProviders")
    val fromProvidersSymbols =  TypeRepr.of[P].typeSymbol.companionClass.methodMember("fromProviders")
    if (fromProvidersSymbols.isEmpty) then
      println("TypeRepr.of[P]: "+TypeRepr.of[P].show)
      println(s"TypeRepr.of[P].typeSymbol: ${TypeRepr.of[P].typeSymbol}")
      println(s"TypeRepr.of[P].typeSymbol.companionClass: ${TypeRepr.of[P].typeSymbol.companionClass}")
      println(s"TypeRepr.of[P].typeSymbol.companionClass.methodMembers: ${TypeRepr.of[P].typeSymbol.companionClass.methodMembers}")



      report.throwError(s"Cannot find fromProviders method in ${TypeRepr.of[P].show}")
    val fromProvidersSymbol = fromProvidersSymbols.head
    var retval = true
    for {t <- tupleTypes} {
      val tProvider = TypeRepr.of[P].appliedTo(t)
      Implicits.search(tProvider) match
        case failure: ImplicitSearchFailure =>
          // impossible, it will be success.
          report.error(s"Cannot find ${tProvider.show}: ${failure.explanation}")
        case success: ImplicitSearchSuccess =>
          // if this is 'this ?
          success.tree match
            case Apply(TypeApply(fun, targs), args@List(p1, ti)) if (fun.symbol == fromProvidersSymbol) =>
              if !(p1.tpe =:= p.asTerm.tpe) then
                report.error(s"Provider for ${t.show} in ${p.show} is not necessory: ${success.tree.show} provider will be used")
                retval = false
            case _ =>
              report.error(s"Provider for ${t.show} is not needed in ${p.show}, it will be resolved as: ${success.tree.show}")
              retval = false
    }
    Expr(retval)
  }

}
