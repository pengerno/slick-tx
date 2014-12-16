package no.penger.db

trait DummyTransactionBoundary
  extends TransactionBoundary
  with DummyTransactionAware {

  override def transaction: Transaction =
    new Transaction {
      override def readOnly[A](f: Tx[RO] => A): A = f(DummyTransaction)
      override def readWrite[A](f: Tx[RW] => A): A = f(DummyTransaction)
    }
}
