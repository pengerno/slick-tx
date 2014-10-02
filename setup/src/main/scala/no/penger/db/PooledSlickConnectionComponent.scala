package no.penger.db

import java.util.Properties

import org.apache.tomcat.jdbc.pool.{DataSource, PoolProperties}
import org.slf4j.Logger

trait PooledSlickConnectionComponent
  extends SlickTransactionBoundary {

  protected def logger: Logger

  def setupPooledDb(dbUrl: String,
                    username: String,
                    password: String,
                    defaultAutoCommit: Boolean,
                    testWhileIdle: Boolean,
                    validationQuery: String,
                    minIdle: Int,
                    maxIdle: Int,
                    // optionals
                    maxActive: Option[Int] = None,
                    minEvictableIdleTimeMillis: Option[Int] = None,
                    suspectTimeout: Option[Int] = None,
                    timeBetweenEvictionRunsMillis: Option[Int] = None) = {

    import profile.simple._

    val p = new PoolProperties()
    p setUrl                dbUrl
    p setDriverClassName    driverClassName
    p setUsername           username
    p setPassword           password
    p setDefaultAutoCommit  defaultAutoCommit
    p setTestWhileIdle      testWhileIdle
    p setValidationQuery    validationQuery
    p setMinIdle            minIdle
    p setMaxIdle            maxIdle

    /* optional parameters */
    maxActive                     foreach p.setMaxActive
    minEvictableIdleTimeMillis    foreach p.setMinEvictableIdleTimeMillis
    suspectTimeout                foreach p.setSuspectTimeout
    timeBetweenEvictionRunsMillis foreach p.setTimeBetweenEvictionRunsMillis

    val dbProps = new Properties()
    dbProps.setProperty("dumpQueriesOnException", "true")
    p setDbProperties dbProps

    logger.info("Configured pooled database connection to " + dbUrl)

    val ds = new DataSource(p)
    SlickTransaction(Database.forDataSource(ds))
  }
}