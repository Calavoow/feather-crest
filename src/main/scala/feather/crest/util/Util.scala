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

package feather.crest.util

import java.net.URLDecoder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

object Util {

	/**
	 * Recover a Failure of f up to `times`.
 *
	 * @param times The number of times to retry (thus total calls = 1 + retries)
	 * @param f The function to obtain the Try of T.
	 * @tparam T The Try type
	 * @return A Try of the given function f.
	 */
	def retry[T](times: Int)(f: ⇒ Try[T]) : Try[T] = {
		val firstTry = f
		// Try recovering with f, `times` times.
		Range(0,times).foldLeft(firstTry) { (cur, _) ⇒
			cur.recoverWith({case NonFatal(ex) ⇒ f})
		}
	}

	def retryFuture[T](times: Int)(f: ⇒ Future[T])(implicit ec: ExecutionContext) : Future[T] = {
		val firstTry = f
		// Try recovering with f, `times` times.
		Range(0,times).foldLeft(firstTry) { (cur, _) ⇒
			cur.recoverWith({case NonFatal(ex) ⇒ f})
		}
	}

	class SkipException extends RuntimeException
	def skippableCache[T](default: ⇒ T)(f: ⇒ T): T = {
		try {
			f
		} catch {
			case skipE: SkipException ⇒ default
		}
	}
	def skipNone[T](f: ⇒ Option[T]) : Option[T] = {
		val res = f
		if(res.isEmpty) throw new SkipException
		else res
	}

	def uriQueryToMap(params: String) : Map[String,String] = {
		params.split("&").map { param =>
			val splitParam = param.split("=")
			URLDecoder.decode(splitParam(0), "UTF-8") -> URLDecoder.decode(splitParam(1), "UTF-8")
		}.toMap
	}
}
