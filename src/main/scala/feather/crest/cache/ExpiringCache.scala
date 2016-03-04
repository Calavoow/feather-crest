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
