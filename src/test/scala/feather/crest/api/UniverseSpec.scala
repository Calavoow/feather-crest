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

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class UniverseSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	behavior of "Universe"

	it should "get solarsystem from the Root" in {
		val futSystems = for(
			r <- Root.public();
			systems <- r.systems.follow()
		) yield systems

		whenReady(futSystems) { systems =>
			// Check no pagination
			systems.totalCount should equal (systems.items.size)
		}
	}

	it should "get regions, constellations, systems, and planets" in {
		implicit val patienceConfig = PatienceConfig(timeout = 15 seconds)
		val reg = for(
			r <- Root.public();
			regions <- r.regions.follow();
			// Take only the first 10 regions and follow the links.
			// map Seq[Future] -> Future[Seq] with Future.sequence.
			selectedRegions <- Future.sequence(regions.items.take(10).map(_.follow()));
			constellations <- Future.sequence(selectedRegions.flatMap(_.constellations).take(10).map(_.follow()));
			systems <- Future.sequence(constellations.flatMap(_.systems).take(10).map(_.follow()));
			planets <- Future.sequence(systems.flatMap(_.planets).take(10).map(_.follow()))
		) yield (regions, selectedRegions, constellations, systems, planets)


		whenReady(reg) { case(regions, selectedRegions, constellations, systems, planets) =>
			// Check for no pagination
			regions.totalCount should equal(regions.items.size)

			selectedRegions should not be empty
			constellations should not be empty
			systems should not be empty
			planets should not be empty

			selectedRegions.foreach { region =>
				region.constellations should not be empty
			}

			constellations.foreach { constellation =>
				constellation.systems should not be empty
			}

			systems.foreach { system =>
				system.planets should not be empty
			}

			planets.foreach { planet =>
				planet.name should not be empty
			}

		}
	}

}
