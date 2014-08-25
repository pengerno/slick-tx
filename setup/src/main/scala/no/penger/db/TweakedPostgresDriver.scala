package no.penger.db

import com.github.tminglei.slickpg.PgDateSupportJoda

import scala.slick.driver.PostgresDriver

object TweakedPostgresDriver extends PostgresDriver with PgDateSupportJoda {
  object Implicits extends DateTimeImplicits
}

