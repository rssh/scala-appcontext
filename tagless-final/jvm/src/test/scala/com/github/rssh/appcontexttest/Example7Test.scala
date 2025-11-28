package com.github.rssh.appcontexttest

import scala.compiletime.requireConst
import com.github.rssh.appcontext.*
import cats.effect.*
import cats.syntax.all.*
import cps.*
import cps.monads.catsEffect.{*, given}

object Example7TestTF {

  type IOResource = [X] =>> Resource[IO, X]

  type Sql = String
  extension (sc: StringContext)
    def sql(args: Any*): Sql = sc.s(args *)

  trait Connection extends AutoCloseable {
    def execute(sql: Sql): IO[String]
  }

  
  
  object DBPool {
    def connection: Resource[IO, Connection] = Resource.make(IO(new Connection {
      def execute(sql: String): IO[String] = IO.delay(s"result of ${sql}")

      def close(): Unit = println("close connection")
    }))(cn => {
      IO.delay(cn.close())
    })
  }

  trait Logger {
    def log(msg: String): IO[Unit]
  }

  case class User(name: String, email: String)

  class UserDatabase(using UserDatabase.DependenciesProviders) {

    requireConst(AppContextAsyncProviders.checkAllAreNeeded)

    def insert(user: User): IO[User] = {
      AppContextAsyncProvider.get[IOResource, Connection].use { conn =>
        AppContextAsyncProvider.get[IOResource, Logger].use { logger =>
          conn.execute(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
            .flatMap(_ => logger.log(s"inserted ${user}"))
        }
      }.map(_ => user)
    }

  }


  object UserDatabase extends AppContextProviderModule[UserDatabase] {

      type DependenciesProviders = AppContextAsyncProviders[IOResource, (Connection, Logger)]

  }

}

class Example7Test extends munit.FunSuite {

  import Example7TestTF.*

  test("example of async provider") {
    import cats.effect.unsafe.implicits.global

    given Logger with {
      def log(msg: String): IO[Unit] = IO.delay {
        println(s"log: ${msg}")
      }
    }

    given AppContextAsyncProvider[IOResource, Connection] with {
      def get: Resource[IO, Connection] = DBPool.connection
    }
    
    val db = AppContext[UserDatabase]

    val user = User("John", "john@example.com")
    val result = db.insert(user).unsafeRunSync()

  }

}

