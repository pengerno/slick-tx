package no.penger.db

/**
 * This trait is to be implemented by abstract repositories
 */
trait TransactionAware {
  type Tx
}

/**
 * To be implemented by concrete database repositories
 */
trait SlickTransactionAware
  extends TransactionAware {
  self: SlickProfile =>

  type Tx = self.profile.simple.Session
}


/**
 * To be implemented by abstract services
 */
trait TransactionBoundary
  extends TransactionAware {

  def transaction: Transaction

  trait Transaction {
    def readOnly[A](f: Tx => A): A
    def readWrite[A](f: Tx => A): A
  }
}

/**
 * The abstract services will need to be made concrete by mixing in this trait
 */
trait SlickTransactionBoundary
  extends TransactionBoundary
  with SlickTransactionAware
  with SlickProfile {

  def transaction: SlickTransaction

  case class SlickTransaction(database: profile.simple.Database) extends Transaction {
    def readOnly[A](f: Tx => A): A = database.withSession((s: Tx) => f(s))
    def readWrite[A](f: Tx => A): A = database.withTransaction((s: Tx) => f(s))
  }
}

/**
 * This trait is how we wire the database into the application
 */
trait SlickProfile {
  val profile: slick.driver.JdbcDriver
  val driverClassName: String
}