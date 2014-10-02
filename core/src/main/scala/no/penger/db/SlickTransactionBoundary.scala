package no.penger.db

/**
 * The abstract services will need to be made concrete by mixing in this trait
 */
trait SlickTransactionBoundary
  extends TransactionBoundary
  with SlickTransactionAware
  with SlickProfile {

  def transaction: SlickTransaction

  case class SlickTransaction(database: profile.simple.Database) extends Transaction {
    def readOnly[A](f: Tx => A): A  = database.withSession((s: Tx) => f(s))
    def readWrite[A](f: Tx => A): A = database.withTransaction((s: Tx) => f(s))
  }
}
