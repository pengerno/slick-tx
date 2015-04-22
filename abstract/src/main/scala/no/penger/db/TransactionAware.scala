package no.penger.db

import scala.language.higherKinds
/**
 * This trait is to be implemented by abstract repositories
 */
trait TransactionAware {
  sealed trait TxType
  trait RO extends TxType
  trait RW extends RO

  type Tx[+T]
}
