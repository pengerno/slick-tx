package no.penger.db

object Profile {
  def apply(url: String) =
    if (url.startsWith("jdbc:postgres"))
      (TweakedPostgresDriver, "org.postgresql.Driver")
    else if (url.startsWith("jdbc:h2"))
      (TweakedH2Driver, "org.h2.Driver")
    else
      sys.error(s"unknown driver for url $url")
}