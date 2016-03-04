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
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"corporation" should "fetch alliances, alliance and corp" in {
		val alliance = for(
			r <- Root.authed();
			firstAlliances <- r.alliances.construct(auth).head;
			alli <- firstAlliances.items.head.follow(auth)
		) yield (firstAlliances,alli)

		whenReady(alliance) { case (alliances, alli) =>
			alli.id.toString should equal(alli.id_str)
			alli.executorCorporation.id.toString should equal(alli.executorCorporation.id_str)
		}
	}

	it should "fetch killmail of page 24 of wars" in {
		implicit val patienceConfig = PatienceConfig(timeout = 40 seconds)
		val futKillMails = for(
			r <- Root.authed();
			wars <- r.wars.construct(auth).drop(24).head;
			war <- Future.traverse(wars.items)(_.follow(auth));
			kills <- Future.sequence(war.map(_.killMailsLink.follow(auth))); // Fetch only the first page of killmails
			kmList <- Future.traverse(kills.flatMap(_.items))(_.follow(auth))
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
			r <- Root.authed();
			wars <- r.wars.follow(auth);
			w <- wars.items.head.follow(auth)
		) yield w

		whenReady(war) { w =>
			w.id.toString should equal (w.id_str)
			w.allyCount.toString should equal (w.allyCount_str)
		}
	}

	it should "get sov structures" in {
		val root = Root.authed(auth)

		val structures = for(
			r <- root;
			structs <- r.sovereignty.structures.follow(auth)
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
		val root = Root.authed(auth)

		val campaigns = for(
			r <- root;
			camps <- r.sovereignty.campaigns.follow(auth)
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
