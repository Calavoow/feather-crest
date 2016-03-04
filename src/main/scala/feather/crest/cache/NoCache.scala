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

	override def keys: Set[Any] = Set.empty

	override def ascendingKeys(limit: Option[Int]): Iterator[Any] = Iterator.empty
}
