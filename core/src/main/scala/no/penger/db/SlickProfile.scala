package no.penger.db

/**
 * This trait is how we wire the database into the application
 */
trait SlickProfile {
  val profile:         slick.driver.JdbcDriver
  val driverClassName: String
}
