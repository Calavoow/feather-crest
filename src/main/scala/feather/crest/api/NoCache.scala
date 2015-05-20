package feather.crest.api

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache.Cache

/**
 * A cache that does nothing.
 */
object NoCache extends Cache {
	override def get[V](key: String): Future[Option[V]] = Future.successful(None)

	override def put[V](key: String, value: V, ttl: Option[Duration]): Future[Unit] = Future.successful()

	override def remove(key: String): Future[Unit] = Future.successful()
}
