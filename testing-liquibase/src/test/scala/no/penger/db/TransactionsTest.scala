package no.penger.db

import java.util.UUID

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.FunSuite

import scala.collection.mutable

/* an abstract repository */
trait UploadRepoComponent extends TransactionAware {
  case class UploadId(id: UUID)
  case class Upload(id: UploadId, filename: String, contents: Array[Byte])
  def uploadRepo: UploadRepo

  trait UploadRepo {
    def register(upload: Upload)(implicit tx: Tx): Unit
    def unregister(id: UploadId)(implicit tx: Tx): Boolean
    def lookup(id: UploadId)(implicit tx: Tx): Option[Upload]
  }
}

/* the repository backed by a mutable map and with no dependency on slick */
trait UploadRepoDummyComponent extends UploadRepoComponent with DummyTransactionAware {
  object uploadRepo extends UploadRepo{
    val uploads = mutable.Map[UploadId, Upload]()

    override def register(upload: Upload)(implicit tx: Tx) =
      uploads(upload.id) = upload

    override def lookup(id: UploadId)(implicit tx: Tx) =
      uploads.get(id)

    override def unregister(id: UploadId)(implicit tx: Tx) =
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

    override def register(upload: Upload)(implicit tx: Tx) =
      Uploads.insert(upload)

    override def lookup(id: UploadId)(implicit tx: Tx) =
      Uploads.filter(_.id === id).firstOption

    override def unregister(id: UploadId)(implicit tx: Tx) =
      Uploads.filter(_.id === id).delete > 0
  }
}

/* this service has a notion of operating on transactions, but slick is abstracted away */
trait UploadServiceComponent extends UploadRepoComponent with TransactionBoundary {
  object uploadService {
    def register(upload: Upload): Either[String, Unit] =
      /* some random logic we would like to test */
      if (upload.contents.isEmpty) 
        Left("Contents can not be empty")
      else if (upload.contents.length > 1337) 
        Left("Contents is too big")
      else transaction.readOnly(implicit tx => Right(uploadRepo.register(upload)))

    def unregister(id: UploadId): Boolean =
      transaction.readOnly(implicit tx => uploadRepo unregister id)

    def lookup(id: UploadId): Option[Upload] =
      transaction.readOnly(implicit tx => uploadRepo lookup id)
  }
}

abstract class TestUploadServiceComponent extends FunSuite with UploadServiceComponent {
  test("test upload service") {
    assert(Left("Contents can not be empty") ===
      uploadService.register(Upload(UploadId(UUID.randomUUID()), "filename", Array.ofDim(0)))
    )

    assert(Left("Contents is too big") ===
      uploadService.register(Upload(UploadId(UUID.randomUUID()), "filename", Array.ofDim(1338)))
    )

    val id = UploadId(UUID.randomUUID())

    assert(Right(()) ===
      uploadService.register(Upload(id, "filename", Array.ofDim(42)))
    )

    assert(true === uploadService.lookup(id).isDefined)
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
  with LiquibaseH2TransactionComponent
  with LazyLogging

class TestUploadServiceDummyComponent
  extends TestUploadServiceComponent
  with UploadRepoDummyComponent
  with DummyTransactionBoundary