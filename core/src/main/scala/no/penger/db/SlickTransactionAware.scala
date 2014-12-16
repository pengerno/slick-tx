package no.penger.db

/**
 * To be implemented by concrete database repositories
 */
trait SlickTransactionAware
  extends TransactionAware {
  self: SlickProfile =>

  final override type Tx[+T] = self.profile.simple.Session
}



