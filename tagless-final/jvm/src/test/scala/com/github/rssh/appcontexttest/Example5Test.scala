package com.github.rssh.appcontexttest


import cats.effect.*
import cats.syntax.all.*
import com.github.rssh.appcontext.{InAppContext, *}
import cps.*
import cps.monads.catsEffect.{*, given}



object Example5TestTF {

  type IOResource = [X]=>>Resource[IO,X]

  type Sql = String
  extension (sc: StringContext)
    def sql(args: Any*): Sql = sc.s(args *)

  trait Connection extends AutoCloseable {
    def execute[F[_]:Async](sql: Sql): F[String]
  }

  object Connection {
    given AppContextAsyncProvider[IOResource,Connection] with {
      def get: Resource[IO,Connection] = DBPool.connection
    }
  }

  object DBPool {
    def connection: Resource[IO, Connection] = Resource.make(IO(new Connection {
      def execute[F[_]:Async](sql: String): F[String] = summon[Async[F]].pure(s"result of ${sql}")
      def close(): Unit = println("close connection")
    }))(cn => { IO.delay(cn.close()) })
  }

  case class User(name: String, email: String)

  trait UserDatabase[F[_]:InAppContext[Connection *: EmptyTuple]:Async] {
    def insert(user: User): F[User]
  }
  object UserDatabase {

    given  [F[_]:InAppContext[Connection *: EmptyTuple]:Async]: AppContextProvider[UserDatabase[F]] with {
      val v = new UserDatabase[F] {
        def insert(user: User): F[User] = {
          for{
            conn <- InAppContext.get[F, Connection]
            _ <- conn.execute(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
          } yield user
        }
      }
      override def get: UserDatabase[F] = v
    }

  }

  trait EmailSender {
    def sendEmail[F[_]:Async](email: String, subject: String, body: String): F[Unit]
  }

  object EmailSender {
    given AppContextProvider[EmailSender] with {
      val v = new EmailSender {
        def sendEmail[F[_]:Async](email: String, subject: String, body: String): F[Unit] = {
          summon[Async[F]].pure(println(s"send email to ${email} with subject ${subject} and body ${body}"))
        }
      }
      override def get: EmailSender = v
    }
  }

  class SubscriptionService[F[_]:InAppContext[(UserDatabase[F], EmailSender)]:Async] {
    def subscribe(user: User): F[Unit] = {
      for {
        userDb <- InAppContext.get[F, UserDatabase[F]]
        emailSender <- InAppContext.get[F, EmailSender]
        _ <- userDb.insert(user)
        _ <- emailSender.sendEmail[F](user.email, "Welcome", "Welcome to our service")
      } yield ()
    }
  }


}

class Example5Test extends munit.FunSuite {

  import Example5TestTF.*

  type IOResource = [X]=>>Resource[IO,X]

  test("insert user") {


    val userDatabase = summon[AppContextProvider[UserDatabase[IOResource]]].get

    val user = User("John", "john@example.com")
    //implicit val printCode = cps.macros.flags.PrintCode
    //implicit val printTree = cps.macros.flags.PrintTree
    val program = userDatabase.insert(user)
    import cats.effect.unsafe.implicits.global
    program.use{ result =>
      IO {
        assert(result == user)
      }
    }.unsafeRunSync()
  }

  test("subscribe user ") {
    val subscriptionService = SubscriptionService[IOResource]()
    val user = User("John", "john@example.com")
    val program = subscriptionService.subscribe(user)
    import cats.effect.unsafe.implicits.global
    program.use{
      _ => IO {
        assert(true)
      }
    }.unsafeRunSync()



  }

}

