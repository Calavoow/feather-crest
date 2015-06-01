package feather.crest.cache

import spray.caching.Cache

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait ExpiringCache[V] extends Cache[V] {
	/**
	 * Returns either the cached Future for the given key or evaluates the given value generating
	 * function producing a `Future[V]`.
	 *
	 * The value has an infinite expiry time.
	 */
	override def apply(key: Any, genValue: () ⇒ Future[V])(implicit ec: ExecutionContext): Future[V] =
		apply(key, genValue, Duration.Inf)

	def apply(key: Any, genValue: () ⇒ Future[V], timeToLive: Duration)(implicit ec: ExecutionContext): Future[V]
}
