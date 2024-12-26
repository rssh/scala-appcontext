package com.github.rssh.appcontext


trait GenericComponent

trait Dependency

@appContextCacheClass[GenericComponent]
class ConcreteComponent(
                         using ConcreteComponent.DependenciesProviders
                       ) extends GenericComponent {

  def doSomething(): Unit =
    val dependency = AppContext[Dependency]
    println(s"Doing something, dependency= ${dependency}, second dependency=${AppContext[Dependency]}")

}

object ConcreteComponent extends AppContextProviderModule[ConcreteComponent]{

  type DependenciesProviders = AppContextProviders[(AppContext.Cache, Dependency)]

  //given (using providers: DependenciesProviders): AppContextProvider[ConcreteComponent] with
  //   def get: ConcreteComponent =
  //     AppContext[AppContext.Cache].getOrCreate(ConcreteComponent(using providers))



}


class AppContextNameTests extends munit.FunSuite {

  test("AppContextName") {
    given Dependency = new Dependency {}
    given AppContext.Cache = AppContext.newCache
    val c1 = AppContext[ConcreteComponent]
    val c2 = AppContext[ConcreteComponent]
    println(s"c1=${c1}, c2=${c2}")
    assert(c1 eq c2)
  }

}
