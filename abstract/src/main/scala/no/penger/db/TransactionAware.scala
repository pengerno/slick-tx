package no.penger.db

/**
 * This trait is to be implemented by abstract repositories
 */
trait TransactionAware {
  type Tx
}
