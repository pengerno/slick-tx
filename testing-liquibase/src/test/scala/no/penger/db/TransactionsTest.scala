package no.penger.db

import java.util.UUID

import org.scalatest.FunSuite
import org.scalactic.TypeCheckedTripleEquals
import org.slf4j.LoggerFactory

import scala.collection.mutable

/* an abstract repository */
trait UploadRepoComponent extends TransactionAware {
  case class UploadId(id: UUID)
  case class Upload(id: UploadId, filename: String, contents: Array[Byte])
  def uploadRepo: UploadRepo

  trait UploadRepo {
    def register(upload: Upload)(implicit tx: Tx[RW]): Unit
    def unregister(id: UploadId)(implicit tx: Tx[RW]): Boolean
    def lookup(id: UploadId)(implicit tx: Tx[RO]): Option[Upload]
  }
}

/* the repository backed by a mutable map and with no dependency on slick */
trait UploadRepoDummyComponent extends UploadRepoComponent with DummyTransactionAware {
  object uploadRepo extends UploadRepo{
    val uploads = mutable.Map[UploadId, Upload]()

    override def register(upload: Upload)(implicit tx: Tx[RW]) =
      uploads(upload.id) = upload

    override def lookup(id: UploadId)(implicit tx: Tx[RO]) =
      uploads.get(id)

    override def unregister(id: UploadId)(implicit tx: Tx[RW]) =
      uploads.remove(id).isDefined
  }
}

/* a concrete, database backed implementation */
trait UploadRepoDbComponent extends UploadRepoComponent with SlickTransactionAware {
  this: SlickProfile =>

  object uploadRepo extends UploadRepo {
    import profile.simple._
    implicit val mapper = MappedColumnType.base[UploadId, UUID](_.id, UploadId)

    class UploadT(tag: Tag) extends Table[Upload](tag, "uploads") {
      def id       = column[UploadId]   ("id")
      def filename = column[String]     ("filename")
      def contents = column[Array[Byte]]("contents")

      def * = (id, filename, contents) <> (Upload.tupled, Upload.unapply)
    }
    val Uploads = TableQuery[UploadT]

    override def register(upload: Upload)(implicit tx: Tx[RW]) =
      Uploads.insert(upload)

    override def lookup(id: UploadId)(implicit tx: Tx[RO]) =
      Uploads.filter(_.id === id).firstOption

    override def unregister(id: UploadId)(implicit tx: Tx[RW]) =
      Uploads.filter(_.id === id).delete > 0
  }
}

/* this service has a notion of operating on transactions, but slick is abstracted away */
trait UploadServiceComponent extends UploadRepoComponent with TransactionBoundary {
  sealed trait Error
  case object EmptyContent extends Error
  case object TooBigContent extends Error
  val MAX_SIZE = 1337

  object uploadService {
    def register(filename: String, contents: Array[Byte]): Either[Error, UploadId] =
      contents.length match {
        case 0                 => Left(EmptyContent)
        case n if n > MAX_SIZE => Left(TooBigContent)
        case _                 =>
          val id     = UploadId(UUID.randomUUID())
          val upload = Upload(id, filename, contents)

          transaction.readWrite(implicit tx => uploadRepo.register(upload))
          Right(id)
      }

    def unregister(id: UploadId): Boolean =
      transaction.readWrite(implicit tx => uploadRepo unregister id)

    def lookup(id: UploadId): Option[Upload] =
      transaction.readOnly(implicit tx => uploadRepo lookup id)
  }
}

abstract class TestUploadServiceComponent extends FunSuite with UploadServiceComponent with TypeCheckedTripleEquals {
  val filename = "filename"

  test("empty"){
    assert(Left(EmptyContent) === uploadService.register(filename, Array.ofDim(0)))
  }
  test("too big") {
    assert(Left(TooBigContent) === uploadService.register(filename, Array.ofDim(MAX_SIZE + 1)))
  }
  test("success case"){
    val contents = Array.ofDim[Byte](42)
    uploadService.register(filename, contents) match {
      case Left(e)   => fail(s"didnt expect error $e")
      case Right(id) => uploadService.lookup(id) match {
        case Some(found) =>
          assert(contents === found.contents)
          assert(filename === found.filename)
        case _           => fail(s"couldnt lookup registered upload $id")
      }
    }
  }
  test("unregister"){
    val nonExisting = UploadId(UUID.randomUUID())
    assert(!uploadService.unregister(nonExisting), "unregister should have returned false")

    uploadService.register(filename, Array.ofDim(42)) match {
      case Left(e)   => fail(s"didnt expect error $e")
      case Right(id) => assert(uploadService.unregister(id), "unregister should have returned true")
                        assert(uploadService.lookup(id).isEmpty, s"should not be able to lookup unregistered id $id")
    }
  }
}

/**
 * wire up the two test applications:
 *  - TestUploadServiceDbComponent backed by slick/H2
 *  - TestUploadServiceDummyComponent backed by a mutable map
 */
class TestUploadServiceDbComponent
  extends TestUploadServiceComponent
  with UploadRepoDbComponent
  with LiquibaseH2TransactionComponent{
  override val log = LoggerFactory.getLogger(classOf[TestUploadServiceDbComponent])
}

class TestUploadServiceDummyComponent
  extends TestUploadServiceComponent
  with UploadRepoDummyComponent
  with DummyTransactionBoundary