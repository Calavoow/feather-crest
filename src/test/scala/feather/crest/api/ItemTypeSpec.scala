package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import feather.crest.api.CrestLink.CrestProtocol._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import feather.crest.api.TwitterConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class ItemTypeSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 20 seconds)

	"itemTypes" should "fetch an itemtype" in {
		val itemTypes = for(
			r <- Root.fetch();
			// Follow the link the the itemTypes page
			itemTypes <- r.itemTypes.follow(auth);
			/**
			 * The itemtypes are split over multiple pages (there are 30k+ of them),
			 * thus we create an asynchronous collection over all itemtype pages
			 * (a Twitter [[com.twitter.concurrent.Spool]]), and concatenate all items.
			 **/
			allTypes <- twitterToScalaFuture(itemTypes.paramsIterator(auth, retries=2).map(_.items).reduceLeft(_ ++ _));
			// Follow the link to the first [[feather.crest.models.ItemType]] page.
			headItemType <- allTypes.head.follow(auth)
		) yield (itemTypes, allTypes, headItemType)

		whenReady(itemTypes) { case (itemTypes, allTypes, headItemType) =>
			// Check if no pagination
			allTypes.size should equal(itemTypes.totalCount)

			allTypes.head.name should equal(headItemType.name)

			// Lets check the first item type
			itemTypes.items.head should equal (NamedCrestLink[ItemType]("https://crest-tq.eveonline.com/types/0/","#System"))
		}
	}

	it should "fetch an itemcategory" in {
		val categories = for(
			r <- Root.fetch();
			iCats <- r.itemCategories.follow(auth);
			headCat <- iCats.items.head.follow(auth)
		) yield (iCats, headCat)


		whenReady(categories) { case(cats, headCat) =>
			// Check if no pagination
			cats.items.size should equal(cats.totalCount)

			cats.items.head should equal(headCat.name)
		}
	}

	it should "fetch an itemGroup" in {
		val groups = for(
			r <- Root.fetch();
			gs <- r.itemGroups.follow(auth);
			allGroups <- twitterToScalaFuture(gs.paramsIterator(auth).map(_.items).reduceLeft(_ ++ _));
			headGroup <- allGroups.head.follow(auth)
		) yield (gs, allGroups, headGroup)

		whenReady(groups) { case (gs, allGroups, headGroup) =>
			// Check if no pagination
			allGroups.size should equal(gs.totalCount)

			allGroups.head.name should equal(headGroup.name)
		}
	}


	it should "be able to fetch the itemtype page of itemtype 984" in {
		implicit val patienceConfig = PatienceConfig(timeout = 5 seconds)
		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		val ham2 = for(
				r <- root;
				itemTypes <- r.itemTypes.follow(auth);
				hammerhead2 <- itemTypes.items(984).follow(auth)
			) yield {
					hammerhead2
				}

		whenReady(ham2) { hammerhead2 =>
			hammerhead2 should equal (ItemType("Hammerhead II", "Medium Scout Drone"))
		}
	}
}
