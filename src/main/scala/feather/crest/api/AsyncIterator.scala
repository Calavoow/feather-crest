package feather.crest.api

import com.typesafe.scalalogging.LazyLogging

import scala.TraversableOnce
import scala.collection.generic.{CanBuildFrom, GenericTraversableTemplate}
import scala.collection.mutable.ListBuffer
import scala.collection.{TraversableOnce, TraversableLike, mutable}
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
abstract class AsyncIterator[T](implicit ec: ExecutionContext)
	extends Traversable[Future[T]]
	with TraversableLike[Future[T], AsyncIterator[T]]
	with LazyLogging {

	def hasNext: Future[Boolean]

	def next(): Future[T]

	/**
	 * Build a [[scala.collection.Seq]] Collection over this traversable.
	 *
	 * Useful for applying more advanced functions, such as map, reduce, etc.
	 * Inspired by [[scala.concurrent.Future.traverse]]
	 *
	 * @return
	 */
	/*
	def traverse: Future[Seq[T]] = {
		this.foldLeft(Future.successful(Seq.newBuilder[T])) { (fr, a) =>
			for (r <- fr; b <- a) yield (r += b)
		}.map(_.result())
	}
	 */

	override def foreach[U](f: Future[T] ⇒ U): Unit = {
		/**
		 * Apply F to every next().
		 *
		 * No arguments so stack is small, even if not tail-recursive.
		 */
		def applyF(): Unit = {
			hasNext.foreach { hNext: Boolean =>
				if(hNext) {
					val nxt = next()
					logger.trace(s"Next is: $nxt")
					nxt.onSuccess {
						case x => logger.trace(s"Next success: $x")
					}
					f(nxt)
					applyF()
				}
			}
		}
		applyF()
	}

	override protected[this] def newBuilder: mutable.Builder[Future[T], AsyncIterator[T]] = {
		AsyncIterator.newBuilder[T]
	}
}

object AsyncIterator extends LazyLogging {
	def newBuilder[A](implicit ec: ExecutionContext) : mutable.Builder[Future[A], AsyncIterator[A]] = {
		new mutable.Builder[Future[A], AsyncIterator[A]] {
			val buffer = mutable.ListBuffer[Future[A]]()

			override def +=(elem: Future[A]): this.type = {
				buffer += elem
				logger.trace("Appended element")
				this
			}

			override def result(): AsyncIterator[A] = {
				logger.trace("Creating result")
				new AsyncIterator[A]() {
					implicit val executionContext = ec
					val iterator = buffer.iterator

					override def next(): Future[A] = iterator.next()

					override def hasNext: Future[Boolean] = Future.successful(iterator.hasNext)
				}
			}

			override def clear(): Unit = buffer.clear()
		}
	}

	implicit def canBuildFrom[A](implicit ec: ExecutionContext): CanBuildFrom[AsyncIterator[_], Future[A], AsyncIterator[A]] = {
		logger.debug("Custom canbuildfrom")
		new CanBuildFrom[AsyncIterator[_], Future[A], AsyncIterator[A]] {
			override def apply(): mutable.Builder[Future[A], AsyncIterator[A]] = newBuilder[A]

			override def apply(from: AsyncIterator[_]): mutable.Builder[Future[A], AsyncIterator[A]] = newBuilder[A]
		}
	}
}
