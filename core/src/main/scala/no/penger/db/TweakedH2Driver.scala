package no.penger.db

import scala.slick.driver.H2Driver

/**
 * A H2 driver that removes quotes, this was necessary for us to make it work
 * see https://github.com/slick/slick/issues/166
 */
object TweakedH2Driver extends H2Driver {
  override def quoteIdentifier(id: String) = id
}
