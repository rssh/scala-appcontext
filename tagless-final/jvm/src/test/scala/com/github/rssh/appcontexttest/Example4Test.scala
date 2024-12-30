package com.github.rssh.appcontexttest


import cats.effect.*
import cats.syntax.all.*
import com.github.rssh.appcontext.*
import cps.*
import cps.monads.catsEffect.{given,*}



object Example4TestTF {

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

  trait UserDatabase {
    def insert[F[_]:InAppContext[Connection *: EmptyTuple]:Async](user: User): F[User]
  }
  object UserDatabase {

    given  AppContextProvider[UserDatabase] with {
      val v = new UserDatabase {
        def insert[F[_]:InAppContext[Connection *: EmptyTuple]:Async](user: User): F[User] = {
          for{
            conn <- InAppContext.get[F, Connection]
            _ <- conn.execute(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
          } yield user
        }
      }
      override def get: UserDatabase = v
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

  class SubscriptionService {
    def subscribe[F[_]:InAppContext[(UserDatabase, EmailSender, Connection)]:Async](user: User): F[Unit] = {
      for {
        userDb <- InAppContext.get[F, UserDatabase]
        emailSender <- InAppContext.get[F, EmailSender]
        _ <- userDb.insert[F](user)
        _ <- emailSender.sendEmail[F](user.email, "Welcome", "Welcome to our service")
      } yield ()
    }
  }


}

class Example4Test extends munit.FunSuite {

  import Example4TestTF.*

  test("insert user") {
     val userDatabase = summon[AppContextProvider[UserDatabase]].get
     val user = User("John", "john@example.com")
     val program = userDatabase.insert[IOResource](user)
     import cats.effect.unsafe.implicits.global
     program.use{ result =>
        IO {
          assert(result == user)
        }
     }.unsafeRunSync()
  }

  test("new subscriber ") {

    val subscriptionService = SubscriptionService()

    val user = User("John", "john@example.com")
    val program = subscriptionService.subscribe[IOResource](user)
    import cats.effect.unsafe.implicits.global
    program.use{ _ =>
      IO {
        assert(true)
      }
    }.unsafeRunSync()


  }

}
