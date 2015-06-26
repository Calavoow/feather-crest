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
	 * Construct an iterable with parameters over the given type T, which iterates throught the CREST.
	 *
	 * The user should *not* have to use this function. Use `authedIterable` instead.
	 *
	 * @param params The parameters to make a crest call with
	 * @param auth The authentication token
	 * @param retries The number of retries.
	 * @return An Iterable over T.
	 */
	def paramsIterator(params: Map[String, String] = Map.empty)
	                  (auth: Option[String], retries: Int = 1)
	                  (implicit ec: ExecutionContext)
	: Spool[T] = {
		def fill(currentPage: Future[T], rest: Promise[Spool[T]]) {
			currentPage foreach { cPage =>
				cPage.next match {
					case Some(nextLink) =>
						val nextPage = nextLink.follow(auth)
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
		fill(Future.successful(self), rest)
		self *:: rest

		/*
		new AsyncIterator[T]() {
			var firstPage: Boolean = true
			var currentPage: Future[T] = Future.successful(self)

			override def hasNext: Future[Boolean] = currentPage.map { page =>
				page.next.isDefined
			}

			override def next(): Future[T] = {
				// The first next() should return the self, but not fetch a next page yet.
				val newPage = if( firstPage ) {
					firstPage = false
					currentPage
				} else {
					// Otherwise follow the link to the next page.
					currentPage.flatMap { page =>
						// Assume the next link exists
						val nextPageLink = page.next.get
						Util.retryFuture(retries) {
							logger.debug(s"Trying to follow next link: $nextPageLink")
							nextPageLink.follow(auth, params)
						}
					}
				}
				currentPage = newPage
				newPage
			}
		}
		*/
	}
}

