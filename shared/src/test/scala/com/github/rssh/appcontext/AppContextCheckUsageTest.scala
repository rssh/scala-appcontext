package com.github.rssh.appcontext




class AppContextCheckUsageTest extends munit.FunSuite {



   test("AppContextProviders.checkAllAreNeeded macros  shpuld check - are all dependencies needed") {

     val errors:List[scala.compiletime.testing.Error] = scala.compiletime.testing.typeCheckErrors {
     """
       trait MyComponentSODependency {
         def me: String
       }

       object MyComponentSODependency {
         given AppContextProvider[MyComponentSODependency] with
           def get: MyComponentSODependency = new MyComponentSODependency {
             override def me: String = "MyComponentSODependency:FromModule"
           }
       }

       class MyComponentSO(using MyComponentSO.DependenciesProviders) {

         //
         require(AppContextProviders.checkAllAreNeeded)

         def doSomething(): String = {
           val dependency = AppContext[MyComponentSODependency]
           //println(s"Doing something, dependency= ${dependency}")
           s"MyComponentSO: dep ${dependency.me}"
         }

       }

       object MyComponentSO extends AppContextProviderModule[MyComponentSO] {
         type DependenciesProviders = AppContextProviders[(AppContext.Cache, MyComponentSODependency)]
         //checkUsageDP
       }

       val myLocalDep = new MyComponentSODependency:
          override def me: String = "MyComponentSODependency:Local"

       given AppContextProvider[MyComponentSODependency] with
         def get: MyComponentSODependency = myLocalDep

       val appContextCache = AppContext.newCache
       val c2 = new MyComponentSO(using AppContextProviders.of((appContextCache, myLocalDep)))

       val res = c2.doSomething()
       """
     }

     //println(s"errors=${errors}")
     assert(!errors.isEmpty)
     assert(errors.head.message.contains("is not needed"))

   }

   test("when dependenciea are really needed, macro should not report any errors") {

    val errors: List[scala.compiletime.testing.Error] = scala.compiletime.testing.typeCheckErrors {
      """
         trait MyComponentSODependency {
           def me: String
         }

         object MyComponentSODependency {
           //given AppContextProvider[MyComponentSODependency] with
           //  def get: MyComponentSODependency = new MyComponentSODependency {
           //    override def me: String = "MyComponentSODependency:FromModule"
           //  }
         }

         class MyComponentSO(using MyComponentSO.DependenciesProviders) {

           //
           require(AppContextProviders.checkAllAreNeeded)

           def doSomething(): String = {
             val dependency = AppContext[MyComponentSODependency]
             //println(s"Doing something, dependency= ${dependency}")
             s"MyComponentSO: dep ${dependency.me}"
           }

         }

         object MyComponentSO extends AppContextProviderModule[MyComponentSO] {
           type DependenciesProviders = AppContextProviders[(AppContext.Cache, MyComponentSODependency)]
           //checkUsageDP
         }

         {
          val myLocalDep = new MyComponentSODependency:
            override def me: String = "MyComponentSODependency:Local"

          given AppContextProvider[MyComponentSODependency] with
           def get: MyComponentSODependency = myLocalDep

          val appContextCache = AppContext.newCache
          val c2 = new MyComponentSO(using AppContextProviders.of((appContextCache, myLocalDep)))
          val res = c2.doSomething()
         }
         """
    }

    //println(s"errors=${errors}")
    assert(errors.isEmpty)

  }

  

}
