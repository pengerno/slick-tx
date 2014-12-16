package no.penger.db

/**
 * The abstract services will need to be made concrete by mixing in this trait
 */
trait SlickTransactionBoundary
  extends TransactionBoundary
  with SlickTransactionAware
  with SlickProfile {

  override def transaction: SlickTransaction

  case class SlickTransaction(database: profile.simple.Database) extends Transaction {
    override def readOnly[A](f: Tx[RO] => A): A  = database.withSession((s: Tx[RO]) => f(s))
    override def readWrite[A](f: Tx[RW] => A): A = database.withTransaction((s: Tx[RW]) => f(s))
  }
}
