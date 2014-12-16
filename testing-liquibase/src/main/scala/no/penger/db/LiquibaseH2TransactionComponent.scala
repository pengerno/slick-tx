package no.penger.db

import java.util.UUID

import no.penger.db.Cache.LiquibaseInfo
import org.slf4j.Logger

import scala.collection.mutable
import scala.ref.WeakReference

private [db] object Cache {

  class LiquibaseInfo{
    val appliedTags = mutable.ArrayBuffer.empty[String]
  }

  var cached: WeakReference[(AnyRef, LiquibaseInfo)] = WeakReference(null)
  val cacheLock     = new Object
}

trait LiquibaseH2TransactionComponent extends H2TransactionComponent {
  import profile.simple._

  val log: Logger

  /* override these if necessary */
  def liquibaseTags      = Seq("test")
  def liquibaseChangelog = "changelog/master.xml"

  /*
    This is useful to set to true if you need concurrent access to the database.
    Mind you that it comes with a performance penalty
  */
  def mvcc               = false

  /* Override this if your test cannot share database */
  @deprecated("Please dont write tests that cannot share database", "2014-12-16")
  def isolation          = false

  private def dbString(db: Database): String = db.toString.split("@").last
  private def isIsolated = isolation || mvcc
  private def dbId = if (isIsolated) UUID.randomUUID().toString else "test"

  private def createDb(): Database = {
    val t0 = System.currentTimeMillis

    val db = Database.forURL(
      url    = s"jdbc:h2:mem:$dbId;DB_CLOSE_DELAY=-1" + (if (mvcc) ";MVCC=true" else ""),
      user   = "sa",
      driver = driverClassName
    )
    
    val td = System.currentTimeMillis - t0

    log.warn(s"created new H2 instance (${dbString(db)}, " +
      s"existedCached: ${Cache.cached.get.isDefined}, " +
      s"isolationRequested: $isolation || $mvcc) in $td ms"
    )

    db
  }

  override lazy val db: Database = {
    val (ret: Database, lb: LiquibaseInfo) =
      if (isIsolated)
        (createDb(), new LiquibaseInfo)
      else Cache.cacheLock.synchronized {
        Cache.cached.get match {
          case Some((cdb: Database, l)) ⇒
            log.warn(s"Reusing db ${dbString(cdb)}")
            (cdb, l)
          case _                       ⇒
            val created = (createDb(), new LiquibaseInfo)
            Cache.cached = WeakReference(created)
            created
        }
    }

    ret.synchronized{
      val newTags = (liquibaseTags.toSet -- lb.appliedTags.toSet).toSeq

      if (newTags.nonEmpty){
        val t0 = System.currentTimeMillis
        log.warn(s"Applying liquibase tags $newTags to db ${dbString(ret)}")

        ret.withSession { tx =>
          Schema.update(tx.conn, liquibaseChangelog, liquibaseTags)
        }
        lb.appliedTags.appendAll(newTags)

        val td = System.currentTimeMillis - t0
        log.warn(s"Applied liquibase tags $newTags to db ${dbString(ret)} in $td ms")
      }
    }

    ret
  }

}