package no.penger.db

import java.util.UUID

trait LiquibaseH2TransactionComponent extends H2TransactionComponent {
  import profile.simple._

  /* override these if necessary */
  val liquibaseTags      = Seq("test")
  val liquibaseChangelog = "changelog/master.xml"

  override lazy val db = {
    val dbId = UUID.randomUUID().toString

    val db = Database.forURL(
      url = s"jdbc:h2:mem:$dbId;DB_CLOSE_DELAY=-1",
      user = "sa",
      driver = driverClassName)

    db.withSession{ s:Session =>
      Schema.update(s.conn, liquibaseChangelog, liquibaseTags)
    }
    db
  }
}