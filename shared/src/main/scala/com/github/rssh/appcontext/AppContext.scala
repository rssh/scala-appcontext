package com.github.rssh.appcontext

import scala.quoted.*

/**
 * Annotation to mark class as a key for component cache.
 * Useful for cases when we have multiple implementation of some generic component.
 * i.e.
 * <code>
 *   @appContextCacheClass[UserRepository]
 *   class UserRepositoryTestImpl(using AppContextProviders[...]) extends UserRepository {
 *     ...
 *   }
 * </code>
 * @tparam T
 */
class appContextCacheClass[T] extends scala.annotation.StaticAnnotation


/**
 * Application context is a way, to resolve components dependencies.
 * Each component should provide an AppContextProvider, and
 * AppContext[Component] will instantiate this components with dependencies.
 */
object AppContext  {

   /**
    * Get component from application context.
    * For this component should have an implicit AppContextProvider.
    * @see AppContextProvider
    * @tparam T - component to instantiate
    * @return
    */
   def apply[T](using AppContextProvider[T]): T =
     summon[AppContextProvider[T]].get

   /**
    * Type for component cache.
    * By convention, if component depends from cache then it's AppContextProvider should check
    * if this component is already in cache and instantiate new (and update cache) only if it's not found.
    */
   opaque type Cache = AppContextCacheMap[String, Any]

    /**
      * Type for cache key.
      * By convention, usually this is a type.show.
      */
   opaque type CacheKey[T] = String

    /**
      * Create empty cache.
      * @return empty cache
      */
   def newCache: Cache = AppContextCacheMap.empty


   inline def cacheKey[T] = ${ cacheKeyImpl[T] }

   private def cacheKeyImpl[T:Type](using Quotes): Expr[CacheKey[T]] = {
      import quotes.reflect.*
      val annotationClass = TypeRepr.of[appContextCacheClass[?]]
      //val keyNameExpr = TypeRepr.of[T].classSymbol.get.annotations.find(_.tpe <:< annotationClass) match
      val keyName: Expr[String] = TypeRepr.of[T].classSymbol match
        case Some(classSym) =>
          classSym.getAnnotation(annotationClass.typeSymbol) match
            case Some(appContextKeyAnnot) =>
              appContextKeyAnnot.tpe.widen match
                case AppliedType(tycon, List(delegated)) =>
                  delegated.asType match
                    case '[d] =>
                       cacheKeyImpl[d]
                    case _ =>
                      report.throwError(s"Can't paese type of application ${delegated.show}")
                case _ =>
                  report.throwError(s"Type of annotation ${appContextKeyAnnot} is not a type application")
            case None => Expr(TypeRepr.of[T].show)
        case None => Expr(TypeRepr.of[T].show)
      keyName
   }

   def customCacheKey[T](key: String): CacheKey[T] = key

   extension  (c: Cache)

    /**
      * Get component from cache.
      * @tparam T - component type
      * @return Some(component) or None if component is not found.
      */
    inline def get[T]: Option[T] =
        c.get(cacheKey[T]).asInstanceOf[Option[T]]

     /**
      * Get component from cache or create new one and update cache.
      * @param value - function to create new component
      * @tparam T - component type
      * @return just created or cached component
      */
    inline def getOrCreate[T](value: => T): T =
       c.getOrElseUpdate(cacheKey[T], value).asInstanceOf[T]

     /**
      * Put component to cache, replacing old if needed
       * @param value
      * @tparam T
      */
    inline def put[T](value: T): Unit =
        c.put(cacheKey[T], value)

    /**
      * Modify component in cache.
      * @param f - function to remap
      * @tparam T - component type
      */ 
    inline def modify[T](f: Option[T] => Option[T]): Unit =
        c.updateWith(cacheKey[T])(v => f(v.asInstanceOf[Option[T]]))
        
    /**
      * Remove component from cache.
      * @tparam T - component type
      */ 
    inline def remove[T]: Unit =
        c.remove(cacheKey[T])  
  
  
}


