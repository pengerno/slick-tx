package no.penger.db

import java.util.UUID

import com.typesafe.scalalogging.slf4j.LazyLogging

object Cache {
  @volatile var cached: Option[Any] = None
}

trait LiquibaseH2TransactionComponent extends H2TransactionComponent {
  this: LazyLogging =>

  import profile.simple._

  /* override these if necessary */
  val liquibaseTags      = Seq("test")
  val liquibaseChangelog = "changelog/master.xml"
  def dbId               = if (isolation) UUID.randomUUID().toString else "test"

  /* especially, override this if your test cannot share database */
  val isolation          = false

  private def dbString(db: Database): String = db.toString.split("@").last

  private def create(): Database = {
    val t0 = System.currentTimeMillis

    val db = Database.forURL(
      url    = s"jdbc:h2:mem:$dbId;DB_CLOSE_DELAY=-1;MVCC=true",
      user   = "sa",
      driver = driverClassName
    )

    db.withSession { s: Session =>
      Schema.update(s.conn, liquibaseChangelog, liquibaseTags)
    }
    val td = System.currentTimeMillis - t0

    logger.warn(s"created new H2 instance (${dbString(db)}, " +
      s"existedCached: ${Cache.cached.isDefined}, " +
      s"isolationRequested: $isolation) with liquibase tags $liquibaseTags in $td ms"
    )

    db
  }

  override lazy val db: Database = {
    if (isolation) create()
    else Cache.synchronized {
      Cache.cached.map(_.asInstanceOf[Database]).getOrElse {
        val db       = create()
        Cache.cached = Some(db)
        db
      }
    }
  }
}