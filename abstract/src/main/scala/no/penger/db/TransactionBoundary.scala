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

    def joiningReadWrite[A](ts: Seq[Tx[RW]])(f: Tx[RW] => A): A = ts.headOption match {
      case Some(tx) ⇒ f(tx)
      case None     ⇒ readWrite(f)
    }

    def joiningReadOnly[A](ts: Seq[Tx[RO]])(f: Tx[RO] => A): A = ts.headOption match {
      case Some(tx) ⇒ f(tx)
      case None     ⇒ readOnly(f)
    }
  }
}
