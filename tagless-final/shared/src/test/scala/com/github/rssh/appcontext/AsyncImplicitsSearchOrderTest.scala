package com.github.rssh.appcontext

import com.github.rssh.appcontext.util.AppContextPure
import com.github.rssh.toymonad.ToyMonad
import cps.*
import cps.syntax.*

import scala.concurrent.ExecutionContext.Implicits.global

object AsyncImplicitsSearchOrderTestClasses {

  case class Dependency1(name: String)

  object Dependency1 {
    // Async provider in companion object - should have lower priority than InAppContext providers
    given [F[_]: CpsEffectMonad]: AppContextAsyncProvider[F, Dependency1] with {
      def get: F[Dependency1] = summon[CpsEffectMonad[F]].pure(Dependency1("Dependency1:From companion"))
    }
  }

  case class Dependency2(name: String)

  object Dependency2 {
    // No provider in companion - must be provided externally
  }

}

class AsyncImplicitsSearchOrderTest extends munit.FunSuite {

  import AsyncImplicitsSearchOrderTestClasses.*

  test("AISO001: AppContextAsyncProviders takes priority over companion-defined AppContextAsyncProvider") {
    // Dependency1 has AppContextAsyncProvider in companion returning "Dependency1:From companion"
    // But when we use InAppContext (AppContextAsyncProviders), the value from providers should take priority

    class Component1[F[_]](using CpsEffectMonad[F], InAppContext[(Dependency1, Dependency2)][F]) {
      def getDependency1Name(): F[String] = {
        summon[CpsEffectMonad[F]].map(InAppContext.get[F, Dependency1])(_.name)
      }

      def getDependency2Name(): F[String] = {
        summon[CpsEffectMonad[F]].map(InAppContext.get[F, Dependency2])(_.name)
      }
    }

    val localDep1 = Dependency1("Dependency1:From InAppContext")
    val localDep2 = Dependency2("Dependency2:From InAppContext")

    // Explicitly create providers with local values
    given providers: AppContextAsyncProviders[ToyMonad, (Dependency1, Dependency2)] =
      AppContextAsyncProviders.of[ToyMonad, (Dependency1, Dependency2)]((localDep1, localDep2))
    val c1 = new Component1[ToyMonad]

    val resultFuture = ToyMonad.run {
      ToyMonad.CpsEffectToyMonad.flatMap(c1.getDependency1Name()) { name1 =>
        ToyMonad.CpsEffectToyMonad.map(c1.getDependency2Name()) { name2 =>
          (name1, name2)
        }
      }
    }

    resultFuture.map { case (name1, name2) =>
      // Key assertion: InAppContext value should win over companion
      assert(name1 == "Dependency1:From InAppContext",
        s"Expected 'Dependency1:From InAppContext' but got '$name1'. " +
        "AppContextAsyncProviders (InAppContext) should take priority over companion-defined AppContextAsyncProvider.")
      assert(name2 == "Dependency2:From InAppContext")
    }
  }

  test("AISO002: fallback to companion AppContextAsyncProvider when no InAppContext in scope") {
    // When there's no InAppContext (AppContextAsyncProviders) in scope,
    // we should fall back to the companion-defined provider

    val resultFuture = ToyMonad.run {
      AppContext.asyncGet[ToyMonad, Dependency1]
    }

    resultFuture.map { d1 =>
      assert(d1.name == "Dependency1:From companion",
        s"Expected 'Dependency1:From companion' but got '${d1.name}'. " +
        "Should fall back to companion-defined AppContextAsyncProvider when no InAppContext in scope.")
    }
  }

  test("AISO003: InAppContext.get uses lookup with correct priority") {
    // Direct test of InAppContext.get ensuring it uses the lookup mechanism

    class Service[F[_]](using CpsEffectMonad[F], InAppContext[(Dependency1 *: EmptyTuple)][F]) {
      def getDep1(): F[Dependency1] = InAppContext.get[F, Dependency1]
    }

    val localDep1 = Dependency1("Dependency1:Local override")

    // Explicitly create providers with local values
    given providers: AppContextAsyncProviders[ToyMonad, Dependency1 *: EmptyTuple] =
      AppContextAsyncProviders.of[ToyMonad, Dependency1 *: EmptyTuple](localDep1 *: EmptyTuple)
    val service = new Service[ToyMonad]

    val resultFuture = ToyMonad.run(service.getDep1())

    resultFuture.map { d1 =>
      assert(d1.name == "Dependency1:Local override",
        s"Expected 'Dependency1:Local override' but got '${d1.name}'. " +
        "InAppContext.get should use lookup that prioritizes InAppContext over companion.")
    }
  }

  test("AISO004: Local sync AppContextProvider preferred over companion async provider") {
    // Test that local AppContextProvider (sync) takes priority over
    // AppContextAsyncProvider defined in companion object.
    // The lookup mechanism converts sync providers via fromSyncProvider at mid priority,
    // which wins over companion async providers at low priority.

    val localDep1 = Dependency1("Dependency1:From local sync provider")

    // Provide sync AppContextProvider - should win over companion async provider
    given AppContextProvider[Dependency1] = AppContextProvider.of(localDep1)

    val resultFuture = ToyMonad.run {
      InAppContext.get[ToyMonad, Dependency1]
    }

    resultFuture.map { d1 =>
      // Local sync provider should win over companion async provider
      assert(d1.name == "Dependency1:From local sync provider",
        s"Expected 'Dependency1:From local sync provider' but got '${d1.name}'. " +
        "Local AppContextProvider should take priority over companion-defined AppContextAsyncProvider.")
    }
  }

  test("AISO005: Local async AppContextAsyncProvider preferred over global sync AppContextProvider") {
    // Test that local AppContextAsyncProvider (in InAppContext) takes priority over
    // global/outer scope AppContextProvider (sync).
    // The fromProviders (from InAppContext) has highest priority.

    class Component[F[_]](using CpsEffectMonad[F], InAppContext[(Dependency2 *: EmptyTuple)][F]) {
      def getDependency2(): F[Dependency2] = InAppContext.get[F, Dependency2]
    }

    // Global sync provider (lower priority)
    given globalSync: AppContextProvider[Dependency2] = AppContextProvider.of(Dependency2("Dependency2:From global sync"))

    // Local async providers via InAppContext (higher priority)
    val localDep2 = Dependency2("Dependency2:From local async InAppContext")
    given providers: AppContextAsyncProviders[ToyMonad, Dependency2 *: EmptyTuple] =
      AppContextAsyncProviders.of[ToyMonad, Dependency2 *: EmptyTuple](localDep2 *: EmptyTuple)

    val component = new Component[ToyMonad]

    val resultFuture = ToyMonad.run(component.getDependency2())

    resultFuture.map { d2 =>
      // Local async provider (via InAppContext) should win over global sync
      assert(d2.name == "Dependency2:From local async InAppContext",
        s"Expected 'Dependency2:From local async InAppContext' but got '${d2.name}'. " +
        "Local InAppContext (async) should take priority over global AppContextProvider (sync).")
    }
  }

}
