package feather.crest.api

import com.twitter.util.{Return, Promise}
import feather.crest.util.Util

import scala.concurrent.{Future, ExecutionContext}
import com.twitter.concurrent.Spool
import Spool.{*::, **::}

/**
 * A trait for the Model classes which adds a method to construct iterables over the CREST.
 *
 * This way it becomes easy to iterate through the CREST and perform functional operations on the collections.
 * For example, one may collect all entries of `x` with a field `.item`:
 *
 * {{{
 * x.authedIterable(oAuth).map(_.items).flatten.toList
 * }}}
 *
 * @tparam T The type of the Model being iterated over.
 */
trait AuthedAsyncIterable[T <: AuthedAsyncIterable[T]] {
	self: T ⇒
	/**
	 * The method that must be implemented by the extender.
	 * @return An option of a link to the next element.
	 */
	def next: Option[CrestLink[T]]

	/**
	 * Construct an iterable with parameters over the given type T, which iterates through the CREST.
	 *
	 * This is usually applied to paginated resources,
	 * where the results are split over multiple pages.
	 *
	 * @param params The parameters to make a crest call with
	 * @param auth The authentication token
	 * @param retries The number of retries.
	 * @return An Iterable over T.
	 */
	def paramsIterator(auth: Option[String], retries: Int = 1, params: Map[String,String] = Map.empty)
	                  (implicit ec: ExecutionContext)
	: Spool[T] = {
		def fill(currentPage: Future[T], rest: Promise[Spool[T]]) {
			currentPage foreach { cPage =>
				cPage.next match {
					case Some(nextLink) =>
						val nextPage = nextLink.follow(auth, retries, params)
						val next = new Promise[Spool[T]]
						rest() = Return(cPage *:: next)
						fill(nextPage, next)
					case None =>
						val emptySpool = new Promise[Spool[T]]
						emptySpool() = Return(Spool.empty[T])
						rest() = Return(cPage *:: emptySpool)
				}
			}
		}
		val rest = new Promise[Spool[T]]
		next match {
			case Some(nxt) => fill(nxt.follow(auth, retries, params), rest)
			case None => rest() = Return(Spool.empty[T])
		}
		self *:: rest
	}
}

