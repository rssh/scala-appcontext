

Simple lightweight dependency injection

See blog-posts:
  - https://github.com/rssh/notes/blob/master/2024_12_09_dependency-injection.md for motivation and core
  - https://github.com/rssh/notes/blob/master/2024_12_30_dependency_injection_tf.md for adoption to tagless-final style
  - https://github.com/rssh/notes/blob/master/2025_12_01_implicit_search_priority.md for fixing implicit search priority

Discord channel #scala-appcontext on business4s:  https://discord.gg/GCMrwpGh 

Analog of UserDatabase example (see https://github.com/scala/scala3/blob/main/tests/run/Providers.scala) looks like:

```Scala

case class User(name: String, email: String)

type Sql = String
extension (sc: StringContext)
    def sql(args: Any*): Sql = sc.s(args*)

class Connection {
   def runQuery(query: Sql): Unit = {
      println(s"Running query: ${query}")
   }
}

trait ConnectionPool {
   def get(): Connection
}

class UserSubscription(using UserSubscription.DependenciesProviders) {

    def subscribe(user: User): Unit =
      AppContext[EmailService].sendEmail(user, "You have been subscribed")
      AppContext[UserDatabase].insert(user)

  }

}

object UserSubscription extends AppContextProviderModule[UserSubscription] {
  type DependenciesProviders = AppContextProviders[(EmailService, UserDatabase)]
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
  }

}

test("AppContext Example3") {

    given ConnectionPool = new ConnectionPool {
      def get(): Connection = new Connection
    }

    given EmailService = new EmailService {}

    val userSubscription = AppContext[UserSubscription]
    userSubscription.subscribe(User("John", "john@example.com"))

}

```


