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
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class IndustrySpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"industry" should "fetch IndustryFacilities" in {
		val facilities = for(
			r <- Root.public();
			fac <- r.industry.facilities.follow()
		) yield fac

		whenReady(facilities) {facils =>
			facils.pageCount.toString should equal(facils.pageCount_str)
			facils.totalCount.toString should equal(facils.totalCount_str)

			// Test if no pagination
			facils.items.size should equal(facils.totalCount)

			facils.items.foreach { facil =>
				facil.facilityID.toString should equal (facil.facilityID_str)
			}

		}
	}

	it should "fetch IndustrySystems" in {
		val systems = for(
			r <- Root.public();
			sys <- r.industry.systems.follow()
		) yield sys

		whenReady(systems) { sys =>
			sys.pageCount.toString should equal(sys.pageCount_str)
			sys.totalCount.toString should equal(sys.totalCount_str)

			// Test if no pagination
			sys.items.size should equal(sys.totalCount)

			sys.items.foreach { system =>
				val firstCostIndex = system.systemCostIndices.head
				firstCostIndex.activityID.toString should equal(firstCostIndex.activityID_str)
			}
		}
	}
}
