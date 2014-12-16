package no.penger.db

trait DummyTransactionAware extends TransactionAware {
  implicit object DummyTransaction
  final override type Tx[+T]   = DummyTransaction.type
}
