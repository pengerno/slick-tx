package no.penger.db

import java.util.UUID

import org.slf4j.LoggerFactory

import scala.ref.WeakReference

private [db] object Cache {
  val CacheLock = new Object
  val LiquibaseLock = new Object
  var cached: WeakReference[AnyRef] = WeakReference(null)
}

trait LiquibaseH2TransactionComponent extends H2TransactionComponent {
  import profile.simple._

  private val l = LoggerFactory.getLogger(classOf[LiquibaseH2TransactionComponent])

  /* override these if necessary */
  val liquibaseTags      = Seq("test")
  val liquibaseChangelog = "changelog/master.xml"
  def dbId               = if (isolation) UUID.randomUUID().toString else "test"

  /* especially, override this if your test cannot share database */
  val isolation          = false

  private def create(): Database = {
    def dbString(db: Database): String = db.toString.split("@").last

    val t0 = System.currentTimeMillis

    val db = Database.forURL(
      url    = s"jdbc:h2:mem:$dbId;DB_CLOSE_DELAY=-1;MVCC=true",
      user   = "sa",
      driver = driverClassName
    )

    Cache.LiquibaseLock.synchronized{
      db.withTransaction { tx: Tx =>
        Schema.update(tx.conn, liquibaseChangelog, liquibaseTags)
      }
    }

    val td = System.currentTimeMillis - t0

    l.warn(s"created new H2 instance (${dbString(db)}, " +
      s"existedCached: ${Cache.cached.get.isDefined}, " +
      s"isolationRequested: $isolation) with liquibase tags $liquibaseTags in $td ms"
    )

    db
  }

  override lazy val db: Database = {
    if (isolation) create()
    else Cache.CacheLock.synchronized {
      Cache.cached.get.map(_.asInstanceOf[Database]).getOrElse {
        val db       = create()
        Cache.cached = WeakReference(db)
        db
      }
    }
  }
}