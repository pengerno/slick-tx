package no.penger.db

/**
 * To be implemented by abstract services
 */
trait TransactionBoundary
  extends TransactionAware {
  def transaction: Transaction

  trait Transaction {
    def readOnly[A](f: Tx[RO] => A): A
    def readWrite[A](f: Tx[RW] => A): A
  }
}
