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

package feather.crest.api

import com.ning.http.client.Response
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import feather.crest.api.CrestLink.CrestCommunicationException
import feather.crest.util.Util
import spray.json._
import scala.concurrent.ExecutionContext
import scala.concurrent.{blocking, ExecutionContext, Future}

case class CrestPost[T: JsonWriter](val href: String) extends LazyLogging {
	def post(input: T, auth: Option[String] = None, retries: Int = 1, params: Map[String,String] = Map.empty)
		(implicit ec: ExecutionContext) : Future[Response] = {
		val basicHeaders = Map(
			"Accept" -> Seq("application/json"),
			"Content-Type" -> Seq("application/json"),
			"User-Agent" -> Seq("feather-crest/0.1")
		)

		// If the auth is set then add it as parameter.
		val headers = auth.foldLeft(basicHeaders)((req, authKey) ⇒ {
			basicHeaders.+(("Authorization", Seq(s"Bearer $authKey")))
		})

		// prepare the POST body
		val body = implicitly[JsonWriter[T]].write(input).compactPrint

		val finalRequest = url(href).secure << body <<? params setHeaders(headers)

		blocking(CrestLink.lock.acquire())
		val responseFut = Util.retryFuture(retries) {
			Http(finalRequest)
		}
		responseFut.onComplete { _ =>
			CrestLink.lock.release()
		}

		val parsedResult = responseFut.map { response ⇒
			response.getStatusCode match {
				case 200 =>
					logger.debug(s"Response status of $href: ${response.getStatusCode}: ${response.getStatusText}")
					response
				case 401 =>
					val message = s"Unauthorized authentication token. CREST response body: ${response.getResponseBody}"
					logger.warn(message)
					throw CrestCommunicationException(401, message)
				case _ =>
					response
			}
		}

		parsedResult.onFailure {
			case x ⇒
				logger.warn(s"Error posting link: $this\n${x.getMessage}\n${x.getStackTrace.mkString("", "\n", "\n")}")
		}
		parsedResult
	}
	override def toString: String = "CrestPost(" + href + ")"
}

