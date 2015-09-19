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
import feather.crest.api.TwitterConverters._
import feather.crest.api.CrestLink.CrestProtocol._
import scala.async.Async.{async,await}

class ModelSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
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
		implicit val patienceConfig = PatienceConfig(timeout = 3 seconds)
		val root = Root.fetch(None)
		whenReady(root) { readyRoot =>
			println(readyRoot)
			readyRoot.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
		}
	}

	it should "be able to fetch itemtypes" in {
		implicit val patienceConfig = PatienceConfig(timeout = 10 seconds)


		logger.info(s"Using authentication: $auth")

		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		val itemTypesPage = root.flatMap(_.itemTypes.follow(auth))
		/**
		 * The itemtypes are split over multiple pages (there are 30k+ of them),
		 * thus we create an asynchronous collection over all itemtype pages
		 * (a Twitter [[com.twitter.concurrent.Spool]]).
		 **/
		val itemTypesSpool = itemTypesPage.map(_.paramsIterator(auth, retries=3))
		// Flatten all itemTypes into one big list.
		val allItemTypes = itemTypesSpool.flatMap { itemType =>
			// Make one large List containing all itemTypes, and its size for testing
			// (And implicitly convert Twitter Future -> Scala Future)
			val totalCount = itemType.map(_.totalCount).head
			itemType.map(_.items).reduceLeft(_ ++ _)
				.map {
				(totalCount, _)
			}
		}
		whenReady(allItemTypes) { case (totalCount, itemTypes) =>
			// Lets check the first item type and the size
			itemTypes.head should equal (NamedCrestLink[ItemType]("https://crest-tq.eveonline.com/types/0/","#System"))
			val duplicates = itemTypes.groupBy(identity).collect { case (x, ys) if ys.size > 1 => x }
			duplicates.foreach(println)
			itemTypes.size should equal (totalCount)
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

	it should "get market data for itemtype Hammerhead II" in {
		implicit val patienceConfig = PatienceConfig(timeout = 10 seconds)
		// As usual get the Root first.
		val root = Root.fetch(None)

		val theForge : Future[Region] = async {
			// Note: no Futures! But everything outside async will stil be asynchronous.
			val aRoot: Root = await(root)
			val regions: Regions = await(aRoot.regions.follow(auth))
			// Note that I use {{.get}} here, which could throw an exception,
			// but simplifies this example.
			val forge: Region = await(regions.items.find(_.name == "The Forge").get
				.follow(auth))

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
			itemTypes.items.find(_.name == "Hammerhead II").get
		}

		// Now we put everything together and get the buy and sell orders.
		val buyAndSell : Future[(MarketOrders, MarketOrders)] = async {
			val aTheForge : Region = await(theForge)
			val aHammerhead2Link : CrestLink[ItemType] = await(hammerhead2Link)

			val ham2Buy : MarketOrders = await(aTheForge.marketBuyLink(aHammerhead2Link)
				.follow(auth))
			val ham2Sell : MarketOrders = await(aTheForge.marketSellLink(aHammerhead2Link)
				.follow(auth))

			// Invariant properties of the order
			(ham2Buy, ham2Sell)
		}

		def invariantOrder(item : MarketOrders.Item): Unit = {
			item.volume should be > 0L
			item.price should be > 0D
			item.minVolume should be > 0L
			item.`type`.name should equal("Hammerhead II")
		}

		whenReady(buyAndSell) {
			case (ham2Buy, ham2Sell) =>
				ham2Buy.items should not be empty // There are always buy order in Jita
				ham2Buy.items.foreach { buyItem =>
					buyItem.buy should be (true)
					invariantOrder(buyItem)
				}
				ham2Sell.items.foreach { sellItem =>
					sellItem.buy should be (false)
					invariantOrder(sellItem)
				}
		}
	}



	it should "get market history for Hammerhead II in Domain" in {
		implicit val patienceConfig = PatienceConfig(timeout = 5 seconds)
		val root = Root.fetch(auth)

		val marketHistory = for(
			r <- root;
			itemLink <- r.itemTypes.follow(auth).map(_.items.find(_.name == "Hammerhead II").get);
			regionLink <- r.regions.follow(auth).map(_.items.find(_.name == "Domain").get);
			history <- MarketHistory.fetch(regionLink, itemLink)(auth)
		) yield {
			history
		}

		whenReady(marketHistory) { history =>
			history.isDefined should equal(true)
			val h = history.get
			h.items.size should be > 100 // There should be many history items.
			h.totalCount should equal(h.items.size)
		}
	}

	it should "get vulnerable sov structures" in {
		implicit val patienceConfig = PatienceConfig(timeout = 10 seconds)
		val root = Root.fetch(auth)

		val structures = for(
			r <- root;
			structs <- r.sovereignty.structures.follow(auth)
		) yield { structs }

		whenReady(structures) { structs =>
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
		implicit val patienceConfig = PatienceConfig(timeout = 10 seconds)
		val root = Root.fetch(auth)

		val campaigns = for(
			r <- root;
			camps <- r.sovereignty.campaigns.follow(auth)
		) yield { camps }

		whenReady(campaigns) { campaigns =>
			campaigns.items.size should equal(campaigns.totalCount)
			campaigns.items.foreach { campaign =>
				campaign.campaignID.toString should equal(campaign.campaignID_str)
				campaign.eventType.toString should equal(campaign.eventType_str)

			}
		}
	}
}
