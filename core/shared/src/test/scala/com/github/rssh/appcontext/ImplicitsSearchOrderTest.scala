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
      def getDependencyName(): String = AppContext[Dependency1].name
    }

    val dep1 = Dependency1("dep1:outer-scope")
    given AppContextProvider[Dependency1] = AppContextProvider.of(dep1)

    val c1 = new Component1(using AppContextProvider.of(Dependency1("dep1:constructor-arg")))

    // Component should use the provider passed to constructor, not the one in outer scope
    assert(c1.getDependencyName() == "dep1:constructor-arg",
      s"Expected 'dep1:constructor-arg' but got '${c1.getDependencyName()}'. " +
      "Component should use the AppContextProvider passed to its constructor.")

    // The outer scope given should still be accessible via summon
    assert(summon[AppContextProvider[Dependency1]].get.name == "dep1:outer-scope",
      "Outer scope given should still resolve to 'dep1:outer-scope'.")

  }

  test("ISO004 component definition with providers") {

    class Component2(using AppContextProviders[(Dependency2 *: EmptyTuple)]) {

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

      def getDependency1Name(): String = AppContext[Dependency1].name
      def getDependency2Name(): String = AppContext[Dependency2].name

      def doSomething(): String = {
        s"${getDependency1Name()}, ${getDependency2Name()}"
      }

    }

    val dep1 = Dependency1("dep1:local")
    val dep2 = Dependency2("dep2:local")
    val c3 = Component3(using AppContextProviders.of(dep1, dep2))

    // Assert that local dependencies are used, not the ones from companion objects
    assert(c3.getDependency1Name() == "dep1:local",
      s"Expected 'dep1:local' but got '${c3.getDependency1Name()}'. " +
      "AppContextProviders should take priority over Dependency1 companion.")
    assert(c3.getDependency2Name() == "dep2:local",
      s"Expected 'dep2:local' but got '${c3.getDependency2Name()}'.")

    println(s"ISO005 ${c3.doSomething()}")

  }

  test("ISO005a component with mixed providers - one from companion, one local") {
    // Tests that when AppContextProviders is generated:
    // - Dependency1 comes from companion (has AppContextProvider in companion)
    // - Dependency2 comes from local given (no AppContextProvider in companion)

    class Component3a(using AppContextProviders[(Dependency1, Dependency2)]) {

      def getDependency1Name(): String = AppContext[Dependency1].name
      def getDependency2Name(): String = AppContext[Dependency2].name
    }

    // Only provide Dependency2 locally; Dependency1 should come from companion
    given AppContextProvider[Dependency2] = AppContextProvider.of(Dependency2("dep2:local"))

    val c = new Component3a

    // Dependency1 should fall back to companion
    assert(c.getDependency1Name() == "Dependency1:From module",
      s"Expected 'Dependency1:From module' but got '${c.getDependency1Name()}'. " +
      "Dependency1 should come from companion when not explicitly provided.")

    // Dependency2 should use local given
    assert(c.getDependency2Name() == "dep2:local",
      s"Expected 'dep2:local' but got '${c.getDependency2Name()}'. " +
      "Dependency2 should come from local given.")

  }

  test("ISO006 layered component definition") {
    // Tests that layered dependencies work:
    // - L1Dependency1's companion requires AppContextProvider[L0Connection]
    // - We provide L0Connection locally
    // - The L1Dependency1 provider in companion uses our local L0Connection

    class Component4(using AppContextProviders[(L1Dependency1, L1Dependency2)]) {

      def getL1Dep1Name(): String = AppContext[L1Dependency1].name
      def getL0ConnectionName(): String = AppContext[L1Dependency1].cn.name
    }

    {
      given AppContextProvider[L0Connection] = AppContextProvider.of(L0Connection("L0Connection:From local"))

      val c = new Component4

      // L1Dependency1 comes from companion (name is "From module")
      assert(c.getL1Dep1Name() == "L1Dependency1:From module",
        s"Expected 'L1Dependency1:From module' but got '${c.getL1Dep1Name()}'")

      // But L0Connection inside L1Dependency1 should be our local one
      assert(c.getL0ConnectionName() == "L0Connection:From local",
        s"Expected 'L0Connection:From local' but got '${c.getL0ConnectionName()}'. " +
        "The local L0Connection should be used by L1Dependency1's companion provider.")
    }

  }

  test("ISO007 layered component definition with l0 override") {
    // Tests that explicitly provided L1Dependency1 via AppContextProviders
    // takes priority over the one generated from companion

    given AppContextProvider[L0Connection] = AppContextProvider.of(L0Connection("L0Connection:From local"))

    class Component4(using AppContextProviders[(L1Dependency1, L1Dependency2)]) {

      def getL1Dep1Name(): String = AppContext[L1Dependency1].name
      def getL0ConnectionName(): String = AppContext[L1Dependency1].cn.name
    }

    {
      val dep1 = L1Dependency1("L1Dependency1:From local", AppContext[L0Connection])
      val dep2 = L1Dependency2("L1Dependency2:From local", AppContext[L0Connection])
      val c = new Component4(using AppContextProviders.of((dep1, dep2)))

      // Both L1Dependency1 and its L0Connection should be from local values
      assert(c.getL1Dep1Name() == "L1Dependency1:From local",
        s"Expected 'L1Dependency1:From local' but got '${c.getL1Dep1Name()}'. " +
        "AppContextProviders should take priority over companion provider.")

      assert(c.getL0ConnectionName() == "L0Connection:From local",
        s"Expected 'L0Connection:From local' but got '${c.getL0ConnectionName()}'")
    }

  }

  test("ISO008 AppContextProviders takes priority over companion-defined AppContextProvider") {
    // Dependency1 has AppContextProvider in companion returning "Dependency1:From module"
    // But when we use AppContextProviders, the value from providers should take priority

    class Component5(using AppContextProviders[Dependency1 *: EmptyTuple]) {
      def getDependency1Name(): String = {
        // This should return the value from AppContextProviders, NOT from Dependency1 companion
        AppContext[Dependency1].name
      }
    }

    val localDep1 = Dependency1("Dependency1:From AppContextProviders")
    val c5 = new Component5(using AppContextProviders.of(localDep1 *: EmptyTuple))

    val result = c5.getDependency1Name()
    println(s"ISO008 result=$result")

    // The key assertion: AppContextProviders value should win over companion
    assert(result == "Dependency1:From AppContextProviders",
      s"Expected 'Dependency1:From AppContextProviders' but got '$result'. " +
      "AppContextProviders should take priority over companion-defined AppContextProvider.")
  }

  test("ISO009 fallback to companion AppContextProvider when no AppContextProviders in scope") {
    // When there's no AppContextProviders in scope, we should fall back to the companion
    val result = AppContext[Dependency1].name
    println(s"ISO009 result=$result")
    assert(result == "Dependency1:From module",
      s"Expected 'Dependency1:From module' but got '$result'. " +
      "Should fall back to companion-defined AppContextProvider when no AppContextProviders in scope.")
  }

}
