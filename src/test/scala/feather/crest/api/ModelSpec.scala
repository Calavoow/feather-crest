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

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}
import feather.crest.models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import feather.crest.api.CrestLink.CrestProtocol._

class ModelSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	"Root" should "be fetchable without auth" in {
		implicit val patienceConfig = PatienceConfig(timeout = 3 seconds)
		val root = Root.authed(None)
		whenReady(root) { readyRoot =>
			println(readyRoot)
			readyRoot.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
		}
	}
}
