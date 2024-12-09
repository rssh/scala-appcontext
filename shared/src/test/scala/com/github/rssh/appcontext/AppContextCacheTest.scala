package com.github.rssh.appcontext

class AppContextCacheTest extends munit.FunSuite {

  trait MyTrait {
    def name: String
  }

  class OtherClass

  test("AppContextCache") {
    val cache = AppContext.newCache
    cache.put(1)
    cache.put[String]("AAAA")
    val myTrait = cache.getOrCreate(new MyTrait {def name="myTrait"})
    cache.put("QQQ")


    assertEquals(cache.get[Int], Some(1))
    assertEquals(cache.get[String], Some("AAAA"))
    assertEquals(cache.get[java.lang.String], Some("QQQ"))
    assertEquals(cache.get[MyTrait].map(_.name), Some("myTrait") )
    assertEquals(cache.get[OtherClass], None)

    cache.modify[Int]{
      case Some(x) => Some(x+1)
      case None => Some(0)
    }
    assertEquals(cache.get[Int], Some(2))

  }

}
