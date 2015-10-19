package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class CorporationSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"corporation" should "fetch alliances, alliance and corp" in {
		val alliance = for(
			r <- Root.fetch();
			alliances <- r.alliances.follow(auth);
			alli <- alliances.items.head.follow(auth)
		) yield alli

		whenReady(alliance) { alli =>
			alli.id.toString should equal(alli.id_str)
			alli.executorCorporation.id.toString should equal(alli.executorCorporation.id_str)
		}
	}

//	it should "fetch a killmail" in {
//		val alliances = for(
//			r <- Root.fetch();
//			as <- r.alliances.follow(auth)
//		) yield as
//
//		whenReady(alliances) { as =>
//			as.paramsIterator(auth).takeWhile()
//		}
//	}

	it should "fetch wars" in {
		val war = for(
			r <- Root.fetch();
			wars <- r.wars.follow(auth);
			w <- wars.items.head.follow(auth)
		) yield w

		whenReady(war) { w =>
			w.id.toString should equal (w.id_str)
			w.allyCount.toString should equal (w.allyCount_str)
		}
	}

	it should "get sov structures" in {
		val root = Root.fetch(auth)

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
		val root = Root.fetch(auth)

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
