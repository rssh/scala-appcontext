package com.github.rssh.appcontext

import scala.quoted.*

class appContextCacheClass[T] extends scala.annotation.StaticAnnotation



object AppContext  {

  
   def apply[T](using AppContextProvider[T]): T =
     summon[AppContextProvider[T]].get

   opaque type Cache = AppContextCacheMap[String, Any]

   opaque type CacheKey[T] = String

   def newCache: Cache = AppContextCacheMap.empty

   inline def cacheKey[T] = ${ cacheKeyImpl[T] }

   def cacheKeyImpl[T:Type](using Quotes): Expr[CacheKey[T]] = {
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
     
    inline def get[T]: Option[T] =
        c.get(cacheKey[T]).asInstanceOf[Option[T]]
     
    inline def getOrCreate[T](value: => T): T =
       c.getOrElseUpdate(cacheKey[T], value).asInstanceOf[T]
  
    inline def put[T](value: T): Unit =
        c.put(cacheKey[T], value)
        
    inline def remove[T]: Unit =
        c.remove(cacheKey[T])  
  
  
}


