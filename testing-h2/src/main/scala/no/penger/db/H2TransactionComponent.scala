package no.penger.db

trait H2TransactionComponent extends SlickTransactionBoundary {

  override val profile         = TweakedH2Driver
  override val driverClassName = "org.h2.Driver"

  def db: profile.simple.Database

  override lazy val transaction = SlickTransaction(db)

  def withRolledbackTransaction[A](f: Tx[RW] => A) = transaction.readWrite {
    s => try {
      f(s)
    } finally s.rollback()
  }
}

