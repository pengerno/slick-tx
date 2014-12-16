package no.penger.db

import scala.language.higherKinds
/**
 * This trait is to be implemented by abstract repositories
 */
trait TransactionAware {
  sealed trait T
  trait RO extends T
  trait RW extends RO

  type Tx[+T]
}
