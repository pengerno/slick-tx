package no.penger.db

import scala.slick.driver.H2Driver

/**
 * A H2 driver that removes quotes, this was necessary for us to make it work
 */
object TweakedH2Driver extends H2Driver {
  override def quoteIdentifier(id: String) = id
}
