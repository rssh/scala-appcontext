package com.github.rssh.appcontext

import scala.collection.mutable.Map as MutableMap

type AppContextCacheMap[K,V] = MutableMap[K, V]

object AppContextCacheMap {
  inline def empty[K,V]: AppContextCacheMap[K,V] = MutableMap.empty[K,V]
}