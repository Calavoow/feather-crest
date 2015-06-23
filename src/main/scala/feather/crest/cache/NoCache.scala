package feather.crest.cache

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * A cache that does nothing.
 */
class NoCache[V] extends ExpiringCache[V] {
	override def apply(key: Any, genValue: () => Future[V], timeToLive: Duration)
	                  (implicit ec: ExecutionContext): Future[V] = {
		genValue()
	}

	override def get(key: Any): Option[Future[V]] = None

	override def clear(): Unit = {}

	override def size: Int = 0

	override def remove(key: Any): Option[Future[V]] = None
}
