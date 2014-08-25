package no.penger.db

import java.sql.Connection

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

object Schema {
  liquibase.logging.LogFactory.getInstance().setDefaultLoggingLevel("warning")

  def update(connection:Connection, changelog: String, tags: Seq[String]){
    val liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), new JdbcConnection(connection))
    liquibase.update(tags.mkString(","))
  }
}
