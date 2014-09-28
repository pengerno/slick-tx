# slick-tx -- An abstraction over transactions

##Using

Normally you want the following for production code:
```"no.penger" %% "tx-core" % "1.0"```

And this for test code:
```"no.penger" %% "tx-testing" % "1.0"```

We also provide a bit more functionality that we use internally in **tx-setup** 
and **tx-testing-liquibase**, feel free to have a look at that too.

#### tx-abstract
Only the transactions abstraction with no dependency on slick. You only want this if have a module that is
transaction aware and shouldn't have a compile-time dependency on slick.

#### tx-core
**tx-abstract** along with a concretization for slick. Most people will want this.

#### tx-setup
**tx-core** along with how we setup postgres database with tomcat connection pooling.

###Testing

#### tx-testing
Contains the dummy concretizations of the transaction abstraction.
This is what you need to write tests backed by mutable maps, for example

#### tx-h2
**tx-testing** along with a dependency on H2, for writing tests that is backed by an in-memory database instance.

#### tx-testing-liquibase
**tx-h2** along with liquibase. This is how we set up up H2 instances with a given liquibase database schema.
It also enables you to specify whether you want a shared database instance or an isolated one.