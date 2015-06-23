package feather.crest.api

import feather.crest.util.Util

import scala.concurrent.{Future, ExecutionContext}

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
	: AsyncIterator[T] = {
		new AsyncIterator[T]() {
			var currentPage: Future[T] = Future.successful(self)

			override def hasNext: Future[Boolean] = currentPage.map { page : T =>
				// Check if it has a link to the next page.
				page.next.isDefined
			}

			override def next(): Future[T] = {
				val nextPage = currentPage.flatMap { page =>
					// Assume that it exists, and the page has a next link.
					val nextPageLink = page.next.get
					Util.retryFuture(retries) {
						logger.debug(s"Following next link: $nextPageLink")
						nextPageLink.follow(auth, params)
					}
				}

				currentPage = nextPage
				nextPage
			}

		}
	}
}
trait NoParamsAsyncIterable[T <: NoParamsAsyncIterable[T]] extends AuthedAsyncIterable[T] {
	self: T ⇒
	def authedIterator(auth: Option[String], retries: Int = 1)(implicit ec: ExecutionContext): AsyncIterator[T]
	= paramsIterator(Map.empty)(auth, retries)
}

