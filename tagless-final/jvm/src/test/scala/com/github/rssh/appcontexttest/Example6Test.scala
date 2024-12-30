package com.github.rssh.appcontexttest


import cats.effect.*
import cats.syntax.all.*
import com.github.rssh.appcontext.{InAppContext, *}
import cps.*
import cps.monads.catsEffect.{*, given}


object Example6TestTF {

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

  case class User(name: String, email: String)

  trait UserDatabase {
    def insert(user: User): IO[User]
  }

  object UserDatabase {

    given (using AppContextAsyncProvider[IOResource, Connection]): AppContextProvider[UserDatabase] with {
      val v = new UserDatabase{
        def insert(user: User): IO[User] = {
          AppContextAsyncProvider.get[IOResource, Connection].use { conn =>
            conn.execute(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
          }.map(_ => user)
        }
      }

      override def get: UserDatabase = v
    }

  }

  trait EmailSender {
    def sendEmail(email: String, subject: String, body: String): IO[Unit]
  }

  class SubscriptionService(using AppContextProviders[(UserDatabase, EmailSender)]) {

    AppContextProviders.checkAllAreNeeded[(UserDatabase, EmailSender)]

    def subscribe(user: User): IO[Unit] = {
      val userDb = AppContext[UserDatabase]
      val emailSender = AppContext[EmailSender]
      for {
        _ <- userDb.insert(user)
        _ <- emailSender.sendEmail(user.email, "Welcome", "Welcome to our service")
      } yield ()
    }
  }


}

class Example6Test extends munit.FunSuite {

  import Example6TestTF.*


  test("insert user") {

    given AppContextAsyncProvider[IOResource, Connection] with
        def get = DBPool.connection

    val userDatabase = AppContext[UserDatabase]

    val user = User("John", "john@example.com")
    //implicit val printCode = cps.macros.flags.PrintCode
    //implicit val printTree = cps.macros.flags.PrintTree
    val program = userDatabase.insert(user)
    import cats.effect.unsafe.implicits.global
    program.unsafeRunSync()
  }

  test("subscribe user ") {

    given AppContextAsyncProvider[IOResource, Connection] = AppContextAsyncProvider.of(DBPool.connection)

    given EmailSender = new EmailSender {
      def sendEmail(email: String, subject: String, body: String): IO[Unit] = {
        IO(println(s"send email to ${email} with subject ${subject} and body ${body}"))
      }
    }

    val subscriptionService = new SubscriptionService()
    val user = User("John", "john@example.com")
    val program = subscriptionService.subscribe(user)
    import cats.effect.unsafe.implicits.global
    program.unsafeRunSync()
  }

}


