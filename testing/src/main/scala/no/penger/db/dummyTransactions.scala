package no.penger.db

trait DummyTransactionAware {
  implicit object DummyTransaction

  type Tx = DummyTransaction.type
}

trait DummyTransactionBoundary
  extends TransactionBoundary
  with DummyTransactionAware {

  override def transaction: Transaction =
    new Transaction {
      override def readOnly[A](f: (Tx) => A): A = f(DummyTransaction)
      override def readWrite[A](f: (Tx) => A): A = f(DummyTransaction)
    }
}