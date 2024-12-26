package com.github.rssh.appcontext


import com.github.rssh.toymonad.ToyMonad
import cps.*
import cps.syntax.*

import java.util.UUID
import scala.util.control.NonFatal

object ExampleTF2 {

  object CorrelationId {

    opaque type Value = String

    def apply(s:String): Value = s

    extension (c:Value)
      def value: String = c
  }

  trait Logger[F[_]:AppContextEffect[CorrelationId.Value *: EmptyTuple]] {

    def info(msg: String): F[Unit]

    def error(msg: String): F[Unit]

  }

  object Logger {

    def apply[F[_]:CpsEffectMonad:AppContextEffect[CorrelationId.Value *: EmptyTuple]]: Logger[F] = new Logger[F] {

      def info(msg: String): F[Unit] = {
        for {
          correlationId <- AppContextEffect.get[F, CorrelationId.Value]
        } yield {
          println(s"info[${correlationId.value}]: ${msg}")
        }
      }

      def error(msg: String): F[Unit] = {
        for {
          correlationId <- AppContextEffect.get[F, CorrelationId.Value]
        } yield {
          println(s"error[${correlationId.value}]: ${msg}")
        }
      }

    }

    given [F[_]:CpsEffectMonad:AppContextEffect[CorrelationId.Value *: EmptyTuple]] :AppContextAsyncProvider[F,Logger[F]] with
      def get: F[Logger[F]] = {
        summon[CpsEffectMonad[F]].pure(Logger.apply[F])
      }

  }

  case class User(id: String, name: String, email: String)

  object BusinessLogic {

    def createUser[F[_]:CpsEffectMonad:AppContextEffect[(Logger[F], Connection)]](name: String, email: String): F[User] = {
      for {
        logger <- AppContextEffect.get[F, Logger[F]]
        _ <- logger.info(s"Creating user with name: ${name} and email: ${email}")
        connection <- AppContextEffect.get[F, Connection]
        userId = UUID.randomUUID().toString
      } yield {
        connection.runQuery(s"insert into users(id, name, email) values (${userId}, ${name}, ${email})")
        User(userId, name, email)
      }
    }

  }

  trait Connection extends AutoCloseable {
    def runQuery(query: String): Unit
    def close(): Unit = ()
  }

  object DB {

    def use[T](f: Connection ?=> ToyMonad[T]):ToyMonad[T] =
      val cn = newConnection()
      try
        ToyMonad.CpsEffectToyMonad.flatMapTry(f(using cn)) { r =>
          cn.close()
          r match
            case scala.util.Success(v) => ToyMonad.pure(v)
            case scala.util.Failure(e) => ToyMonad.error(e)
        }
      catch
        case NonFatal(e) =>
          cn.close()
          ToyMonad.error(e)

    private def newConnection(): Connection = new Connection {
      def runQuery(query: String): Unit = {
        println(s"Running query: ${query}")
      }

      override def close(): Unit = {
        println("Closing connection")

      }
    }

  }

  sealed trait UserCommand
  case class CreateUser(name: String, email: String) extends UserCommand
  case class UpdateUser(id: String, name: String, email: String) extends UserCommand
  case class DeleteUser(id: String) extends UserCommand

  def handleUserCommand(headers: Map[String,String], input: UserCommand): ToyMonad[User] = {
    given CorrelationId.Value = CorrelationId(
      headers.getOrElse("correlationId", throw new IllegalArgumentException("correlationId is required"))
    )
    DB.use{
      input match
        case CreateUser(name, email) =>
          BusinessLogic.createUser[ToyMonad](name, email)
        case UpdateUser(id, name, email) =>
          //BusinessLogic.updateUser[ToyMonad](id, name, email)
          throw new IllegalArgumentException("Not implemented")
        case DeleteUser(id) =>
          //BusinessLogic.deleteUser[ToyMonad](id)
          throw new IllegalArgumentException("Not implemented")
    }

  }



}

class Example2Test extends munit.FunSuite {

  import ExampleTF2.*

  test("ERT001: handleRequest") {
     val v = handleUserCommand(Map("correlationId" -> "123"), CreateUser("John", "john@example.com"))
     ToyMonad.run(v)
  }

}
