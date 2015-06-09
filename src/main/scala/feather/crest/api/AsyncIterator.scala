package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.util.Util

import scala.collection.{mutable, TraversableLike}
import scala.concurrent.{ExecutionContext, Future}

/**
 * An asynchronous iterator is a non-blocking iterator.
 *
 * Because hasNext must have a non-blocking return type (Future[Boolean]),
 * it is not possible to implement Iterable/Iterator.
 * It is however possible to implement foreach.
 *
 * @tparam T The type to iterate over.
 * @param ec The execution context to execute the iterator in.
 *           This parameter must be given already, otherwise Traversable cannot be implemented.
 */
abstract class AsyncIterator[+T](implicit ec: ExecutionContext)
	extends TraversableLike[Future[T], AsyncIterator[T]] with LazyLogging {
	def hasNext: Future[Boolean]

	def next(): Future[T]

	/**
	 * Build an Iterable Collection over this traversable.
	 *
	 * Useful for applying more advanced functions, such as map, reduce, etc.
	 *
	 * @return
	 */
	def reduce : Future[Iterable[T]] = {
		val iterableBuilder = Iterable.newBuilder[T]
		def applyOnce() : Future[Iterable[T]] = {
			hasNext.flatMap { hNext ⇒
				if( hNext ) {
					next().flatMap { n ⇒
						iterableBuilder += n
						applyOnce()
					}
				} else {
					Future.successful(iterableBuilder.result())
				}
			}
		}
		applyOnce()
	}

	override def foreach[U](f: Future[T] ⇒ U): Unit = {
		// var items = 0
		// This is not tail recursive, but the stack is very small per call.
		def applyEach() {
			// items += 1
			hasNext.foreach { hNext ⇒
				if( hNext ) {
					f(next)
					applyEach()
				}
			}
		}
	}

	override protected[this] def newBuilder: mutable.Builder[Future[T], AsyncIterator[T]] = ???

	override def seq: TraversableOnce[Future[T]] = this
}

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
		new AsyncIterator[T] {
			var lastLink: Future[Option[T]] = Future.successful(Some(self))
			// Create an iterator that has a future option of the CrestLink.
			val currentLink = Iterator.iterate[Future[Option[T]]](
				lastLink
			) { futureSelf ⇒
				// Has type Future[Option[T]]
				// Future[Future[Option[T]]] is returned, so flatMap it.
				futureSelf.flatMap { oSelf ⇒
					val res = for (
						curSelf ← oSelf;
						link ← curSelf.next
					) yield Util.retryFuture(retries) {
							link.follow(auth, params)
						}
					// Invert Option[Future[T]] to Future[Option[T]]
					res match {
						case None ⇒ Future.successful(None)
						case Some(fut : Future[T]) ⇒ fut.map(Some.apply _)
					}
				}
			}

			override def hasNext: Future[Boolean] = lastLink.map(_.isDefined)

			override def next(): Future[T] = {
				// Use the last link as "next", and fetch the next one.
				val res = lastLink.map(_.get)
				lastLink = currentLink.next()
				res
			}
		}
	}
}
/*
trait NoParamsAyncIterable[T <: NoParamsAyncIterable[T]] extends AuthedAsyncIterable[T] {
	self: T ⇒
	def authedIterator(auth: Option[String], retries: Int = 1)(implicit ec: ExecutionContext): AsyncIterator[T]
	= paramsIterator(Map.empty)(auth, retries)
}

trait NoParamsIterable[T <: NoParamsIterable[T]] extends IterableLike[Future[T], NoParamsIterable[T]] {
	def pageCount : Int
	def next: Option[CrestLink[T]]

	override def iterator: scala.Iterator[Future[T]] = new Iterator[Future[T]] {
		var page = 1
		override def hasNext: Boolean = page <= pageCount

		override def next(): Future[T] = NoParamsIterable.this.next.get.follow
	}

	override protected[this] def newBuilder: mutable.Builder[Future[T], NoParamsIterable[T]] = ???

	override def seq: scala.TraversableOnce[Future[T]] = ???
}
*/