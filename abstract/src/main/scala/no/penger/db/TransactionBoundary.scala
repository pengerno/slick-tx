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

    def joinReadOnly[A](otx: Option[Tx[RO]])(block: Tx[RO] ⇒ A) = {
      if (otx.isDefined) block(otx.get)
      else transaction readOnly { tx ⇒ block(tx) }
    }

    def joinReadWrite[A](otx: Option[Tx[RW]])(block: Tx[RW] ⇒ A) = {
      if (otx.isDefined) block(otx.get)
      else transaction readWrite { tx ⇒ block(tx) }
    }
  }
}
