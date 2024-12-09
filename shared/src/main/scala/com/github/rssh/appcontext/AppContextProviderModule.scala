package com.github.rssh.appcontext

import scala.quoted.*

trait AppContextProviderModule[T] {

  /**
   * Dependencies providers: AppContextProviders[(T1,T2,...,Tn)], where T1,T2,...,Tn are dependencies.
   */
  type DependenciesProviders
  
  /**
   * Component type, which we provide.
   */
  type Component = T

  
  inline given provider(using dependenciesProvider: DependenciesProviders): AppContextProvider[Component] = ${
    AppContextProviderModule.providerImpl[Component, DependenciesProviders]('dependenciesProvider)
  }

  
  
  inline def checkUsageDP: Unit = ${
     AppContextProviderModule.checkUsageDpImpl[Component, DependenciesProviders]
  }
  
}

object AppContextProviderModule {

  trait CheckUsage[DP] {
    def check: Unit
  }

  def providerImpl[T:Type,DP:Type](dp:Expr[DP])(using Quotes): Expr[AppContextProvider[T]] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]
    //val primaryConstructor = tpe.classSymbol.get.primaryConstructor
    val isTrait = tpe.typeSymbol.flags.is(Flags.Trait)
    if (isTrait) {
      report.errorAndAbort(s"trait is not supported as a component, make ${tpe.show} a class")
    }
    val newExpr = tpe.typeArgs match {
      case Nil => Select.unique(New(Inferred(tpe)),"<init>")
      case targs =>
        TypeApply(Select.unique(New(TypeTree.of[T]),"<init>"),targs.map(Inferred(_)))
    }
    val getExpr = Apply(Apply(newExpr,List(dp.asTerm)),List.empty).asExprOf[T]
    // check - are we have AppConfig.Cache in the dependencies?
    val getWithOptCacheChekExpr = TypeRepr.of[DP].dealias match
      case dpTupled@AppliedType(tycon, List(targsTuple)) if tycon <:< TypeRepr.of[AppContextProviders] =>
            targsTuple match
              case AppliedType(tp1, targs) =>
                val i = targs.indexWhere( _ <:< TypeRepr.of[AppContext.Cache] )
                val expr = if (i >= 0) then
                    val cacheExpr =
                      Apply(
                        TypeApply(Select.unique(dp.asTerm,"unsafeGet"),List(TypeTree.of[AppContext.Cache])),
                        List(Literal(IntConstant(i)))
                      ).asExprOf[AppContext.Cache]
                    '{ ${cacheExpr}.getOrCreate( $getExpr )  }
                else
                    getExpr
                expr
              case _ =>
                getExpr
      case other =>
        if (other <:< TypeRepr.of[AppContext.Cache]) then
          '{ ${dp.asExprOf[AppContext.Cache]}.getOrCreate( $getExpr ) }
        else
          getExpr
    val retval = '{
      new AppContextProvider[T] {
          def get: T = ${getWithOptCacheChekExpr} }
    }
    retval
  }
  

  def checkUsageDpImpl[T:Type, DP:Type](using Quotes): Expr[Unit] = {
    import quotes.reflect.*
    TypeRepr.of[DP].dealias match
      case AppliedType(tycon, List(targsTuple)) if tycon <:< TypeRepr.of[AppContextProviders] =>
        targsTuple match
          case AppliedType(tp1, targs) =>
            for{ targ <- targs } {
              val tProvider = TypeRepr.of[AppContextProvider].appliedTo(targ)
              Implicits.search(tProvider) match
                case failure: ImplicitSearchFailure =>
                  //report.error(s"Cannot find ${tProvider.show}: ${failure.explanation}")
                case success: ImplicitSearchSuccess =>
                  // ok, do nothing
                  report.error(s"Provider for ${targ.show} is not necessory: ${success.tree.show} provider will be used")
            }
          case _ =>
            report.error(s"AppContextProviders should have tuple of dependencies, we have ${targsTuple.show}")
      case oneProvider if oneProvider <:< TypeRepr.of[AppContextProvider[?]] =>
        Implicits.search(oneProvider) match
          case failure: ImplicitSearchFailure =>
            //report.error(s"Cannot find ${oneProvider.show}: ${failure.explanation}")
          case success: ImplicitSearchSuccess =>
            // ok, do nothing
            report.error(s"Provider for ${oneProvider.show} is not necessory: ${success.tree.show} provider will be used")
      case _ =>
        report.error(s"AppContextProviders should have AppContextProviders[T] like type, we have ${TypeRepr.of[DP].show}")
    '{}
  }


}