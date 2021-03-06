/*
 * Feather-Crest is a library that simplifies interaction with the EVE Online API.
 * Copyright (C) 2016 Calavoow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package feather.crest.cache

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import feather.crest.cache.ExpiringLruCache.Entry

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.languageFeature.postfixOps
import scala.util.{Failure, Success}

/**
 * A thread-safe implementation of [[feather.crest.cache.ExpiringCache]].
 *
 * Implementation is largely based on [[spray.caching.SimpleLruCache]].
 * The cache has a defined maximum number of entries it can store. After the maximum capacity has been reached new
 * entries cause old ones to be evicted in a last-recently-used manner, i.e. the entries that haven't been accessed for
 * the longest time are evicted first.
 * In addition this implementation optionally supports time-to-live as well as time-to-idle expiration.
 * The former provides an upper limit to the time period an entry is allowed to remain in the cache while the latter
 * limits the maximum time an entry is kept without having been accessed. If both values are non-zero the time-to-live
 * has to be strictly greater than the time-to-idle.
 * Note that expired entries are only evicted upon next access (or by being thrown out by the capacity constraint), so
 * they might prevent gargabe collection of their values for longer than expected.
 *
 */
class ExpiringLruCache[V](maxCapacity: Long, initialCapacity: Int) extends ExpiringCache[V] {
	private val store = new ConcurrentLinkedHashMap.Builder[Any, Entry[V]]
		.initialCapacity(initialCapacity)
		.maximumWeightedCapacity(maxCapacity)
		.build()

	def get(key: Any): Option[Future[V]] = recGet(key)

	/**
	 * A private version of get, such that it can be optimized for tail recursion, but still overridden.
	 * @param key
	 * @return
	 */
	@tailrec
	private def recGet(key: Any): Option[Future[V]] = store.get(key) match {
		case null ⇒ None
		case entry if (isAlive(entry)) ⇒
			entry.refresh()
			Some(entry.future)
		case entry ⇒
			// remove entry, but only if it hasn't been removed and reinserted in the meantime
			if (store.remove(key, entry)) None // successfully removed
			else recGet(key) // nope, try again
	}

	def apply(key: Any, genValue: () ⇒ Future[V], timeToLive: Duration)(implicit ec: ExecutionContext): Future[V] = {
		def insert() = {
			val newEntry = new Entry(Promise[V](), timeToLive)
			val valueFuture =
				store.put(key, newEntry) match {
					case null ⇒ genValue()
					case entry ⇒
						if (isAlive(entry)) {
							// we date back the new entry we just inserted
							// in the meantime someone might have already seen the too fresh timestamp we just put in,
							// but since the original entry is also still alive this doesn't matter
							newEntry.created = entry.created
							entry.future
						} else genValue()
				}
			valueFuture.onComplete { value ⇒
				newEntry.promise.tryComplete(value)
				// in case of exceptions we remove the cache entry (i.e. try again later)
				if (value.isFailure) store.remove(key, newEntry)
			}
			newEntry.promise.future
		}
		store.get(key) match {
			case null ⇒ insert()
			case entry if (isAlive(entry)) ⇒
				entry.refresh()
				entry.future
			case entry ⇒ insert()
		}
	}

	def remove(key: Any) = store.remove(key) match {
		case null                      ⇒ None
		case entry if (isAlive(entry)) ⇒ Some(entry.future)
		case entry                     ⇒ None
	}

	def keys: Set[Any] = store.keySet().asScala.toSet

	def ascendingKeys(limit: Option[Int] = None) =
		limit.map { lim ⇒ store.ascendingKeySetWithLimit(lim) }
			.getOrElse(store.ascendingKeySet())
			.iterator().asScala

	def clear(): Unit = { store.clear() }

	def size = store.size

	private def isAlive(entry: Entry[V]) : Boolean =
		entry.created + entry.timeToLive.toMillis > System.currentTimeMillis()
}

object ExpiringLruCache {
	class Entry[T](val promise: Promise[T], val timeToLive: Duration) {
		@volatile var created = System.currentTimeMillis()
		@volatile var lastAccessed = System.currentTimeMillis()

		def future = promise.future

		def refresh(): Unit = {
			// we dont care whether we overwrite a potentially newer value
			lastAccessed = System.currentTimeMillis()
		}

		override def toString = future.value match {
			case Some(Success(value)) ⇒ value.toString
			case Some(Failure(exception)) ⇒ exception.toString
			case None ⇒ "pending"
		}
	}
}
