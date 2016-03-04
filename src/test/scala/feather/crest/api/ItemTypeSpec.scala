package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import feather.crest.api.CrestLink.CrestProtocol._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ItemTypeSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 20 seconds)

	"itemTypes" should "fetch an itemtype" in {
		val itemTypes = for(
			r <- Root.authed();
			// Follow the link the the itemTypes page, which is paginated because it has many items (30k+).
			itemTypes <- Future.sequence(r.itemTypes.construct(auth))
			// Follow the link to the first [[feather.crest.models.ItemType]] page.
		) yield itemTypes

		whenReady(itemTypes) { iTypes =>
			// Check if we fetched all itemtypes.
			val allTypes = iTypes.flatMap(_.items)
			allTypes.size should equal(iTypes.head.totalCount)

			// Check the first item type
			allTypes.head.name should equal ("#System")
		}
	}

	it should "fetch an itemcategory" in {
		val categories = for(
			r <- Root.authed();
			iCats <- r.itemCategories.follow(auth);
			headCat <- iCats.items.head.follow(auth)
		) yield (iCats, headCat)


		whenReady(categories) { case(cats, headCat) =>
			// Check if no pagination
			cats.items.size should equal(cats.totalCount)

			cats.items.head.name should equal(headCat.name)
		}
	}

	it should "fetch an itemGroup" in {
		val groups = for(
			r <- Root.authed();
			gs <- Future.sequence(r.itemGroups.construct(auth));
			headGroup <- gs.map(_.items).head.head.follow(auth)
		) yield (gs, headGroup)

		whenReady(groups) { case (gs, headGroup) =>
			val allGroups = gs.map(_.items).reduceLeft(_ ++ _)
			// Check if no pagination
			allGroups.size should equal(gs.head.totalCount)

			allGroups.head.name should equal(headGroup.name)
		}
	}


	it should "be able to fetch the itemtype page of itemtype 984" in {
		implicit val patienceConfig = PatienceConfig(timeout = 5 seconds)
		// Follow the link to the itemtype page.
		val ham2 = for(
			root <- Root.authed();
			itemTypes <- root.itemTypes.follow(auth);
			hammerhead2 <- itemTypes.items(984).follow(auth)
			) yield {
					hammerhead2
				}

		whenReady(ham2) { hammerhead2 =>
			hammerhead2 should equal (ItemType("Hammerhead II", "Medium Scout Drone"))
		}
	}
}
