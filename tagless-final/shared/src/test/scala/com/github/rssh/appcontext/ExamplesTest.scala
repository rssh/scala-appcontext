package com.github.rssh.appcontext

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.rssh.toymonad.ToyMonad
import cps.*
import cps.syntax.*


object ExampleTF1 {

  type Async[F[_]] = CpsAsyncMonad[F]
  type Effect[F[_]] = CpsEffectMonad[F]

  case class User(name: String, email: String)

  trait EmailService {
     def sendEmail[F[_]:Effect](u: User, message:String):F[Unit]
  }

  trait Connection {

  }

  trait ConnectionPool {
     def get():Connection
  }

  trait UserDatabase[F[_]:Effect:AppContextEffect[ConnectionPool *: EmptyTuple]] {
     def insert(user: User):F[User]
  }


  def newSubscriber1[F[_]:AppContextEffect[(UserDatabase[F],EmailService)]:Effect](user: User):F[User] = {
     for{
       db <- AppContextEffect.get[F, UserDatabase[F]]
       u <- db.insert(user)
       emailService <- AppContextEffect.get[F, EmailService]
       _ <- emailService.sendEmail(u, "You have been subscribed")
     } yield u
     
  }



}


class ExamplesTest extends munit.FunSuite {


  test("example of async provider") {
     import ExampleTF1.*

     given EmailService with 
       def sendEmail[F[_]:Effect](u: User, message:String):F[Unit] = {
            summon[Effect[F]].delay{ println("send-email")  }
       }

     class MyLocalDatabase[F[_]:CpsEffectMonad:AppContextEffect[ConnectionPool *: EmptyTuple]] extends UserDatabase[F] {
       def insert(user: User):F[User] = reify[F] {
         val cn = AppContext.async[F, ConnectionPool].await
         println(s"insert ${user} with connection ${cn}, this=${this}")
         user
       }
     }

     class MyLocalConnectionPool extends ConnectionPool {
       def get():Connection = new Connection {}
     }

     given [F[_]:CpsEffectMonad]: AppContextAsyncProvider[F,UserDatabase[F]] with {
       given ConnectionPool = new MyLocalConnectionPool
       val v = new MyLocalDatabase[F]
       override def get: F[UserDatabase[F]] = summon[Effect[F]].delay(v)
     }

     val user = User("John", "john@example.com")
     val r = newSubscriber1[ToyMonad](user)
     val fr = ToyMonad.run(r)
     fr.map{ u =>
        assert(u == user)
     }

  }


}

