package com.github.rssh.appcontext

import com.github.rssh.appcontext.{AppContextProvider, AppContextProviders}

object ImplicitsSearchOrderTestClasses1 {

  case class Dependency1(name: String)

  object Dependency1 {
    given AppContextProvider[Dependency1] = AppContextProvider.of(Dependency1("Dependency1:From module"))
  }

  case class Dependency2(name: String)

  object Dependency2 {
    //given AppContextProvider[Dependency2] = new AppContextProvider {
    //   def get = new Dependency2 {
    //     def name = "Dependency2:From module"
    //   }
    //}
  }

  case class L0Connection(name: String)

  case class L1Dependency1(name: String, cn: L0Connection)
  object L1Dependency1 {
    given (using AppContextProvider[L0Connection]):AppContextProvider[L1Dependency1] =
      AppContextProvider.of(L1Dependency1("L1Dependency1:From module", AppContext[L0Connection]))
  }

  case class L1Dependency2(name: String, cn: L0Connection)
  object L1Dependency2 {
    given (using AppContextProvider[L0Connection]):AppContextProvider[L1Dependency2] =
      AppContextProvider.of(L1Dependency2("L1Dependency2:From module", AppContext[L0Connection]))
  }

}

class ImplicitsSearchOrderTest extends munit.FunSuite {

  import ImplicitsSearchOrderTestClasses1.*

  test("ISO001: given instance in scope is not preffered over module") {

     given dependency1: Dependency1 = Dependency1("From local given")

     val dep = summon[AppContextProvider[Dependency1]]
     //println(s"dep.name=${dep.get.name}")
     assert(dep.get.name == "Dependency1:From module")

  }

  test("ISO002 given instance of AppProvider scope is  preffered over module") {

    given dependency1: Dependency1 = Dependency1("From local given")

    given AppContextProvider[Dependency1] = AppContextProvider.ofGiven

    val dep = summon[AppContextProvider[Dependency1]]
    //println(s"ISO002 dep.name=${dep.get.name}")
    assert(dep.get.name == "From local given")

  }

  test("ISO003 component definition with provider") {

    class Component1(using AppContextProvider[Dependency1]) {
      def doSomething(): String = {
        val dep = AppContext[Dependency1]
        dep.name
      }
    }

    val dep1 = Dependency1("1")
    given AppContextProvider[Dependency1] = AppContextProvider.of(dep1)

    val c1 = new Component1(using AppContextProvider.of(Dependency1("2")))

    println(s"ISO003: c1.doSomething()=${c1.doSomething()}")
    println(s"ISO003 summon[AppContextProvider[Dependency1]].get.name=${summon[AppContextProvider[Dependency1]].get.name}")

  }

  test("ISO004 component definition with providers") {

    class Component2(using AppContextProviders[(Dependency2 *: EmptyTuple)]) {

      assert(AppContextProviders.checkAllAreNeeded)


      def doSomething(): String = {
        val dep = AppContext[Dependency2]
        dep.name
      }
    }

    {

      val dep2 = Dependency2("dep2")
      given ofDep1:AppContextProvider[Dependency2] = AppContextProvider.of(dep2)

      val c2 = new Component2

      //println(c2.doSomething())
      //println(s"summon[AppContextProvider[Dependency1]].get.name=${summon[AppContextProvider[Dependency2]].get.name}")
    }
  }

  test("ISO005 component definition with providers and given") {

    class Component3(using AppContextProviders[(Dependency1, Dependency2)]) {

      //assert(AppContextProviders.checkAllAreNeeded)

      def doSomething(): String = {
        s"${AppContext[Dependency1].name}, ${AppContext[Dependency2].name}"
      }

    }

    val dep1 = Dependency1("dep1:local")
    val dep2 = Dependency2("dep2:local")
    val c3 = Component3(using AppContextProviders.of(dep1, dep2))
    println(s"IS005 ${c3.doSomething()}")

  }

  test("ISO006 layered component definition") {

    class Component4(using AppContextProviders[(L1Dependency1, L1Dependency2)]) {

      def doSomething(): String = {
        val dep = AppContext[L1Dependency1]
        s"${dep.name}, ${dep.cn.name}"
      }
    }

    {
      given AppContextProvider[L0Connection] = AppContextProvider.of(L0Connection("L0Connection:From local"))

      val c = new Component4
      println(s"ISO006 ${c.doSomething()}")
    }

  }

  test("ISO007 layered component definition with l0") {

    given AppContextProvider[L0Connection] = AppContextProvider.of(L0Connection("L0Connection:From local"))

    class Component4(using AppContextProviders[(L1Dependency1, L1Dependency2)]) {

      //assert(AppContextProviders.checkAllAreNeeded)

      def doSomething(): String = {
        val dep = AppContext[L1Dependency1]
        s"${dep.name}, ${dep.cn.name}"
      }
    }

    {
      val dep1 = L1Dependency1("L1Dependency1:From local", AppContext[L0Connection])
      val dep2 = L1Dependency2("L1Dependency2:From local", AppContext[L0Connection])
      val c = new Component4(using AppContextProviders.of((dep1, dep2)))
      println(s"ISO007 ${c.doSomething()}")
    }

  }

}
