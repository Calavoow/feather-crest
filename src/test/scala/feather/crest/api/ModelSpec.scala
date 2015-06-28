package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, FlatSpec}
import Models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import feather.crest.api.TwitterConverters._
import feather.crest.api.CrestLink.CrestProtocol._
import scala.async.Async.{async,await}

class ModelSpec extends FlatSpec with Matchers with LazyLogging {
	/**
	 * Try to read auth.txt in the test/resources folder.
	 *
	 * This file should only contain a string of an authentication token,
	 * such that the authed-crest may be accessed with it.
	 */
	val auth = {
		Option(getClass().getResource("/auth.txt"))
			.map(Source.fromURL)
			.map(_.getLines().next())
	}

	"Root" should "be fetchable without auth" in {
		val root = Root.fetch(None)
		root.onComplete {
			case Failure(ex) => fail("Failed to fetch root", ex)
			case Success(rootRes) =>
				rootRes.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
		}
		Await.ready(root, 10 seconds)
	}

	it should "be able to fetch itemtypes" in {

		logger.info(s"Using authentication: $auth")

		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		val itemTypesPage = root.flatMap(_.itemTypes.follow(auth))
		/**
		 * The itemtypes are split over multiple pages (there are 30k+ of them),
		 * thus we create an asynchronous collection over all itemtype pages
		 * (a Twitter [[com.twitter.concurrent.Spool]]).
		 **/
		val itemTypesSpool = itemTypesPage.map(_.authedIterator(auth, retries=3))
		// Flatten all itemTypes into one big list.
		val allItemTypes = itemTypesSpool.flatMap { itemType =>
			// Make one large List containing all itemTypes
			// (And implicitly convert Twitter Future -> Scala Future)
			itemType.map(_.items).reduceLeft(_ ++ _)
		}
		allItemTypes.foreach { itemTypes =>
			// Lets check the first item type and the size
			itemTypes.head should equal (NamedCrestLink[ItemType]("https://crest-tq.eveonline.com/types/0/","#System"))
			itemTypes.size should be >(350000)
		}

		Await.ready(allItemTypes, 30 seconds)
	}

	it should "be able to fetch the itemtype page of itemtype 2185" in {
		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		for(
			r <- root;
			itemTypes <- r.itemTypes.follow(auth);
			hammerhead2 <- itemTypes.items(2185).link.follow(auth)
		) {
			hammerhead2 should equal (ItemType("Hammerhead II", "Medium Scout Drone"))
		}
	}

	it should "get market data for itemtype 2185" in {
		// As usual get the Root first.
		val root = Root.fetch(None)

		val theForge : Future[Region] = async {
			// Note: no Futures! But everything outside async will stil be asynchronous.
			val aRoot: Root = await(root)
			val regions: Regions = await(aRoot.regions.follow(auth))
			// Note that I use {{.get}} here, which could throw an exception, but simplifies this example.
			val forge: Region = await(regions.items.find(_.name == "The Forge").get
				.link.follow(auth))

			/**
			 * Oops, from the type of {{theForge.marketSellLink}}
			 * we see that we need an CrestLink[ItemType],
			 * so lets handle that in _parallel_ (below).
			 */
			forge
		}

		// Get a link to the Itemtype of Hammerhead II's.
		val hammerhead2Link : Future[CrestLink[ItemType]] = async {
			val aRoot : Root = await(root)
			// Lets fetch the Hammerhead II
			val itemTypes : ItemTypes = await(aRoot.itemTypes.follow(auth))
			itemTypes.items.find(_.name == "Hammerhead II").get.link
		}

		// Now we put everything together and get the buy and sell orders.
		val buyAndSell : Future[(MarketOrders, MarketOrders)] = async {
			val aTheForge : Region = await(theForge)
			val aHammerhead2Link : CrestLink[ItemType] = await(hammerhead2Link)

			val ham2Buy : MarketOrders = await(aTheForge.marketBuyLink(aHammerhead2Link)
				.follow(auth))
			val ham2Sell : MarketOrders = await(aTheForge.marketSellLink(aHammerhead2Link)
				.follow(auth))

			// Print the first buy and sell order
			println(ham2Buy.items.head)
			println(ham2Sell.items.head)

			(ham2Buy, ham2Sell)
		}

		Await.ready(buyAndSell, 10 seconds)
	}
}
