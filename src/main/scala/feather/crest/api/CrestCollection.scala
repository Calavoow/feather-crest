package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.cache.{ExpiringCache, NoCache}
import feather.crest.models._
import feather.crest.api.CrestLink.CrestProtocol._
import spray.json.JsonFormat

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

case class CrestCollection[T : JsonFormat](override val href: String) extends CrestLink[Collection[T]](href) with LazyLogging {
	/**
	  * Construct a `Stream` which lazily pulls items of the paginated resource `Collection[T]`.
	  */
	def construct(auth: Option[String] = None, retries: Int = 1, params: Map[String, String] = Map.empty)
			(implicit ec: ExecutionContext, cache: ExpiringCache[Collection[T]] = new NoCache[Collection[T]]): Stream[Future[Collection[T]]] = {

		// Eagerly fetch the head, required for a Stream.
		val head = this.follow(auth = auth, retries = retries, params = params)

		/**
		 * The stream needs to end when the page no longer has a next member.
		 * To do this we keep taking pages until the next is no longer defined,
		 * however, this would end the stream one page too soon (*on* the page that has page.next == None).
		 * Thus I add a flag to the stream signaling one page after page.next == None that the stream can end.
		 */
		lazy val stream : Stream[Future[(Collection[T], Boolean)]] = head.map(x => (x,false)) #:: stream.map { currentPage =>
			currentPage.flatMap { realizedPage =>
				realizedPage._1.next match {
					case None => Future.successful((realizedPage._1, true))
					case Some(next) => next.follow(auth = auth, retries = retries, params = params).map(x => (x,false))
				}
			}
		}.takeWhile { currentPage =>
			// If the flag is false, keep taking pages.
			!Await.result(currentPage, Duration.Inf)._2
		}
		// Remove the flag
		stream.map(_.map(_._1))
	}

}
