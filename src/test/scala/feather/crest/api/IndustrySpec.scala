package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class IndustrySpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"industry" should "fetch IndustryFacilities" in {
		val facilities = for(
			r <- Root.authed();
			fac <- r.industry.facilities.follow(auth)
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
			r <- Root.authed();
			sys <- r.industry.systems.follow(auth)
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
