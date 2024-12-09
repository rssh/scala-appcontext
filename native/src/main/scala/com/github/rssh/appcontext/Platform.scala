package com.github.rssh.appcontext

import scala.collection.concurrent.TrieMap

type AppContextCacheMap[K,V] = TrieMap[K, V]

object AppContextCacheMap {
  def empty[K,V]: AppContextCacheMap[K,V] = TrieMap.empty[K,V]
}