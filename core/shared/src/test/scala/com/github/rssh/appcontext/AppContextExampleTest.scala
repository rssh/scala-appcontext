package com.github.rssh.appcontext


object ExampleCommon {

  case class User(name: String, email: String)

  type Sql = String
  extension (sc: StringContext)
    def sql(args: Any*): Sql = sc.s(args*)

  class Connection {
    def runQuery(query: Sql): Unit = {
      println(s"Running query: ${query}")
    }
  }

}

/*
 Analog of https://github.com/scala/scala3/blob/main/tests/run/Providers.scala
 usign AppContextProvider  without macroses.
*/
object Example1  {

  import ExampleCommon.*


  class UserSubscription(using AppContextProvider[EmailService],
                               AppContextProvider[UserDatabase]
                        ) {

    def subscribe(user: User): Unit =
      AppContext[EmailService].sendEmail(user, "You have been subscribed")
      AppContext[UserDatabase].insert(user)

  }

  object UserSubscription {
      // boilterplate
      given (using AppContextProvider[EmailService],
                    AppContextProvider[UserDatabase]): AppContextProvider[UserSubscription] with
        def get: UserSubscription = new UserSubscription
  }

  trait EmailService {
    def sendEmail(user: User, message: String): Unit = {
      println(s"Sending email to ${user.email} with message: ${message}")
    }
  }

  trait UserDatabase(using AppContextProvider[ConnectionPool]) {
    def insert(user: User) =
      val conn = AppContext[ConnectionPool].get()
      conn.runQuery(sql"insert into subscribers(name, email) values ${user.name} ${user.email}")
  }
  object UserDatabase {
    given (using AppContextProvider[ConnectionPool]): AppContextProvider[UserDatabase] with
      def get: UserDatabase = new UserDatabase {}
  }

  trait ConnectionPool {
    def get(): Connection
  }

}

object Example2 {

  import ExampleCommon.*

  class UserSubscription(using AppContextProviders[(EmailService, UserDatabase)]) {

    def subscribe(user: User): Unit =
      AppContext[EmailService].sendEmail(user, "You have been subscribed")
      AppContext[UserDatabase].insert(user)
  }

  object UserSubscription {
    // boilterplate
    given (using AppContextProvider[EmailService],
           AppContextProvider[UserDatabase]): AppContextProvider[UserSubscription] with
      def get: UserSubscription = new UserSubscription
  }

  trait EmailService {
    def sendEmail(user: User, message: String): Unit = {
      println(s"Sending email to ${user.email} with message: ${message}")
    }
  }

  trait UserDatabase(using AppContextProvider[ConnectionPool]) {
    def insert(user: User) =
      val conn = AppContext[ConnectionPool].get()
      conn.runQuery(s"insert into subscribers(name, email) values ${user.name} ${user.email}")
  }

  object UserDatabase {
    given (using AppContextProvider[ConnectionPool]): AppContextProvider[UserDatabase] with
      def get: UserDatabase = new UserDatabase {}
  }

  trait ConnectionPool {
    def get(): Connection
  }

}

object Example3 {

  import ExampleCommon.*

  class UserSubscription(using UserSubscription.DependenciesProviders) {

    assert(AppContextProviders.checkAllAreNeeded)
    
    def subscribe(user: User): Unit =
      AppContext[EmailService].sendEmail(user, "You have been subscribed")
      AppContext[UserDatabase].insert(user)

  }


  object UserSubscription extends AppContextProviderModule[UserSubscription] {
    type DependenciesProviders = AppContextProviders[(EmailService, UserDatabase)]
    checkUsageDP
  }

  trait EmailService {
    def sendEmail(user: User, message: String): Unit = {
      println(s"Sending email to ${user.email} with message: ${message}")
    }
  }

  class UserDatabase(using AppContextProvider[ConnectionPool]) {
    def insert(user: User) =
      val conn = AppContext[ConnectionPool].get()
      conn.runQuery(s"insert into subscribers(name, email) values ${user.name} ${user.email}")
  }

  object UserDatabase extends AppContextProviderModule[UserDatabase] {
    type DependenciesProviders = AppContextProvider[ConnectionPool]
    checkUsageDP
  }

  trait ConnectionPool {
    def get(): Connection
  }

}


class AppContextExampleTest extends munit.FunSuite  {

  test("AppContext Example1") {
    import Example1.*
    import ExampleCommon.*

    given ConnectionPool = new ConnectionPool {
      def get(): Connection = new Connection
    }
    given EmailService = new EmailService {}

    val userSubscription = AppContext[UserSubscription]
    userSubscription.subscribe(User("John", "john@example.com"))

  }

  test("AppContext Example2") {
    import Example2.*
    import ExampleCommon.*

    given ConnectionPool = new ConnectionPool {
      def get(): Connection = new Connection
    }

    given EmailService = new EmailService {}
    val userSubscription = AppContext[UserSubscription]
    userSubscription.subscribe(User("John", "john@example.com"))

  }

  test("AppContext Example3") {
    import Example3.*
    import ExampleCommon.*

    given ConnectionPool = new ConnectionPool {
      def get(): Connection = new Connection
    }

    given EmailService = new EmailService {}

    val userSubscription = AppContext[UserSubscription]
    userSubscription.subscribe(User("John", "john@example.com"))

  }


}
