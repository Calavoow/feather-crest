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
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class CorporationSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"corporation" should "fetch alliances, alliance and corp" in {
		val alliance = for(
			r <- Root.public();
			firstAlliances <- r.alliances.construct().head;
			alli <- firstAlliances.items.head.follow()
		) yield (firstAlliances,alli)

		whenReady(alliance) { case (alliances, alli) =>
			alli.id.toString should equal(alli.id_str)
			alli.executorCorporation.id.toString should equal(alli.executorCorporation.id_str)
		}
	}

	it should "fetch killmail of page 24 of wars" in {
		implicit val patienceConfig = PatienceConfig(timeout = 40 seconds)
		val futKillMails = for(
			r <- Root.public();
			wars <- r.wars.construct().drop(24).head;
			war <- Future.traverse(wars.items)(_.follow());
			kills <- Future.sequence(war.map(_.killMailsLink.follow())); // Fetch only the first page of killmails
			kmList <- Future.traverse(kills.flatMap(_.items))(_.follow())
		) yield kmList

		whenReady(futKillMails) { killLists =>
			// At least one km should be tested.
			killLists should not be empty

			killLists.foreach { killmail =>
				killmail.war.isDefined should be(true) // since we retrieve the killmails through wars
				killmail.victim.damageTaken.toString should equal(killmail.victim.damageTaken_str)
			}
		}
	}

	it should "fetch wars" in {
		val war = for(
			r <- Root.public();
			wars <- r.wars.follow();
			w <- wars.items.head.follow()
		) yield w

		whenReady(war) { w =>
			w.id.toString should equal (w.id_str)
			w.allyCount.toString should equal (w.allyCount_str)
		}
	}

	it should "get sov structures" in {
		val root = Root.public()

		val structures = for(
			r <- root;
			structs <- r.sovereignty.structures.follow()
		) yield { structs }

		whenReady(structures) { structs =>
			// All items should already be on this page, because it is assumed to not be paginated.
			structs.items.size should equal(structs.totalCount)

			structs.items.foreach { structure =>
				structure.structureID.toString should equal(structure.structureID_str)
			}

			val nonVulnStruct = structs.items.find { struct =>
				struct.vulnerabilityOccupancyLevel.isEmpty
			}
			println(nonVulnStruct)
		}
	}

	it should "get sov campaigns" in {
		val root = Root.public()

		val campaigns = for(
			r <- root;
			camps <- r.sovereignty.campaigns.follow()
		) yield { camps }

		whenReady(campaigns) { campaigns =>
			// All items should already be on this page, because it is assumed to not be paginated.
			campaigns.items.size should equal(campaigns.totalCount)
			campaigns.items.foreach { campaign =>
				campaign.campaignID.toString should equal(campaign.campaignID_str)
				campaign.eventType.toString should equal(campaign.eventType_str)

			}
		}
	}
}
