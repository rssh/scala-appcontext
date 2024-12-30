package com.github.rssh.appcontext

class Example4Test


import scala.concurrent.ExecutionContext.Implicits.global
import com.github.rssh.toymonad.ToyMonad
import cps.*
import cps.syntax.*


object ExampleTF3 {

  type Async[F[_]] = CpsAsyncMonad[F]
  type Effect[F[_]] = CpsEffectMonad[F]

  type Sql = String
  extension (sc: StringContext)
    def sql(args: Any*): Sql = sc.s(args *)

  case class User(name: String, email: String)

  trait EmailService {
    def sendEmail[F[_]:Effect](u: User, message:String):F[Unit]
  }

  trait Connection {
    def runSql[F[_]:Effect](sql:Sql):F[String]
  }

  trait ConnectionPool {
    def get[F[_]:Effect]():F[Connection]
  }

  trait UserDatabase[F[_]:Effect:InAppContext[ConnectionPool *: EmptyTuple]] {
    def insert(user: User):F[User]
  }

  object UserDatabase {

    given [F[_]:Effect:InAppContext[ConnectionPool *: EmptyTuple]]: AppContextAsyncProvider[F,UserDatabase[F]] with {
      val v = new UserDatabase[F] {
        def insert(user: User):F[User] = {
          for{
            pool <- InAppContext.get[F, ConnectionPool]
            conn <- pool.get()
            _ <- conn.runSql(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
          } yield user
        }
      }
      override def get: F[UserDatabase[F]] = summon[Effect[F]].delay(v)
    }

  }

  def newSubscriber1[F[_]:InAppContext[(UserDatabase[F],EmailService)]:Effect](user: User):F[User] = {
    for{
      db <- InAppContext.get[F, UserDatabase[F]]
      u <- db.insert(user)
      emailService <- InAppContext.get[F, EmailService]
      _ <- emailService.sendEmail(u, "You have been subscribed")
    } yield u

  }



}


class Example3Test extends munit.FunSuite {


  test("example of async provider") {
    import ExampleTF3.*

    given EmailService with
      def sendEmail[F[_]:Effect](u: User, message:String):F[Unit] = {
        summon[Effect[F]].delay{ println("send-email")  }
      }

    class MyLocalConnectionPool extends ConnectionPool {
      def get[F[_]:Effect]():F[Connection] = summon[Effect[F]].delay(new Connection {
        def runSql[F[_]:Effect](sql:Sql):F[String] = {
          summon[Effect[F]].delay(s"run-sql ${sql}")
        }
      })
    }
    given ConnectionPool = new MyLocalConnectionPool
    
    val user = User("John", "john@example.com")
    val r = newSubscriber1[ToyMonad](user)
    val fr = ToyMonad.run(r)
    fr.map{ u =>
      assert(u == user)
    }

  }


}

