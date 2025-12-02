package com.github.rssh.appcontext

import com.github.rssh.appcontext.TupleIndex.OfSubtype
import cps.*

import scala.quoted.*
import scala.util.{NotGiven, boundary}

trait AppContextAsyncProviders[F[_],Xs <: NonEmptyTuple] extends AppContextAsyncProvidersSearch[F,Xs] {


}

object AppContextAsyncProviders {

 /**
  * Accept a tuple of services and create an AppContextAsyncProviders for them.
  * The services are wrapped in pure effect.
  * ```
  *   given pure: AppContextPure[F] = ...
  *   val providers = AppContextAsyncProviders.of[F, (Service1, Service2)](service1, service2)
  *   val depended = new Dependent(using providers)
  * ```
  */
 def of[F[_], T <: NonEmptyTuple](values: T)(using pure: util.AppContextPure[F]): AppContextAsyncProviders[F, T] = {
   val arr = values.productIterator.map { v =>
     AppContextAsyncProvider.of[F, Any](pure.pure(v)): AppContextAsyncProvider[F, ?]
   }.toArray
   new DefaultAppContextAsyncProviders[F, T](arr)
 }

 trait TryBuild[F[_], Xs<:NonEmptyTuple]
 case class TryBuildSuccess[F[_],Xs<:NonEmptyTuple](providers:AppContextAsyncProviders[F,Xs]) extends TryBuild[F,Xs]
 case class TryBuildFailure[F[_],Xs<:NonEmptyTuple](message: String) extends TryBuild[F,Xs]

 transparent inline given tryBuild[F[_],Xs <:NonEmptyTuple]: TryBuild[F,Xs] = ${
   tryBuildImpl[F,Xs]
 }

 inline given build[F[_]:CpsMonad, Xs <: NonEmptyTuple, R <: TryBuild[F,Xs]](using inline trb: R, inline ev: R <:< TryBuildSuccess[F,Xs]): AppContextAsyncProviders[F,Xs] = {
   trb.providers
 }

 protected class DefaultAppContextAsyncProviders[F[_], Xs <: NonEmptyTuple](array: Array[AppContextAsyncProvider[F,?]]) extends AppContextAsyncProviders[F,Xs] {
    def getProvider[T, N <: Int](using TupleIndex.OfSubtype[Xs, T, N]): AppContextAsyncProvider[F,T] =
      array(summon[TupleIndex.OfSubtype[Xs, T, N]].index).asInstanceOf[AppContextAsyncProvider[F,T]]

    def unsafeGetProvider[T](i: Int): AppContextAsyncProvider[F,T] =
      array(i).asInstanceOf[AppContextAsyncProvider[F,T]]
 }

 

 def tryBuildImpl[F[_]:Type, Xs <: NonEmptyTuple:Type](using Quotes): Expr[TryBuild[F,Xs]] = {
   import quotes.reflect.*
   val fType = TypeRepr.of[F]
   val tupleType = TypeRepr.of[Xs]
   val tupleTypes = TupleMacroses.extractTupleTypes(tupleType)
   val retval = boundary[Expr[TryBuild[F,Xs]]] {
      val listResolved = tupleTypes.map { t =>
      val tProvider = TypeRepr.of[AppContextAsyncProvider].appliedTo(List(fType,t))
      Implicits.search(tProvider) match
         case failure: ImplicitSearchFailure =>
           val message = s"Cannot find async provider for ${t.show} (${tProvider.show}): ${failure.explanation}"
           boundary.break('{ TryBuildFailure[F,Xs](${Expr(message)})  })
         case success: ImplicitSearchSuccess =>
           success.tree.asExprOf[AppContextAsyncProvider[F,?]]
      }
      '{
        TryBuildSuccess[F,Xs](
         new DefaultAppContextAsyncProviders[F,Xs](
           ${ Expr.ofSeq(listResolved) }.toArray
        ))
      }
   }
   retval
 }

}
