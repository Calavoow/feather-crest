package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.api.TwitterConverters._
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.async.Async.{async,await}

class MarketSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"marketTypes" should "get market data for itemtype Hammerhead II" in {
		val ham2Orders : Future[(MarketOrders, MarketOrders)] = async {
			// Note: no Futures! But everything outside async will stil be asynchronous.
			val aRoot: Root = await(Root.fetch())
			val regions: Regions = await(aRoot.regions.follow(auth))
			// Note that I use {{.get}} here, which could throw an exception,
			// but simplifies this example.
			val forge: Region = await(regions.items.find(_.name == "The Forge").get
				.follow(auth))

			/**
			 * From the type of [[Region.marketSellLink]]
			 * we see that we need an CrestLink[ItemType],
			 * so lets handle that in _parallel_.
			 *
			 * Async-await will automatically find independent asynchronous requests,
			 * and run them in parallel.
			 */
			val itemTypes : ItemTypes = await(aRoot.itemTypes.follow(auth))
			val ham2Link = itemTypes.items.find(_.name == "Hammerhead II").get


			// Now we put everything together and get the buy and sell orders.
			val ham2Buy : MarketOrders = await(forge.marketBuyLink(ham2Link)
				.follow(auth))
			val ham2Sell : MarketOrders = await(forge.marketSellLink(ham2Link)
				.follow(auth))

			(ham2Buy, ham2Sell)
		}

		def invariantOrder(item : MarketOrders.Item): Unit = {
			item.volume should be > 0L
			item.price should be > 0D
			item.minVolume should be > 0L
			item.`type`.name should equal("Hammerhead II")
		}

		whenReady(ham2Orders) {
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
		val marketHistory = for(
			r <- Root.fetch();
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

	it should "get market groups" in {
		val marketG = for(
			r <- Root.fetch();
			marketGroups <- r.marketGroups.follow(auth);
			marketGroup <- marketGroups.items.head.follow(auth);
			marketType <- marketGroup.typesLink.follow(auth);
			allItemTypes <- marketType.paramsIterator(auth).map(_.items).reduceLeft(_ ++ _)
		) yield (marketGroups, marketType, allItemTypes)

		whenReady(marketG) { case(marketGroups, marketType, allItemTypes) =>
			// Check if not paginated
			marketGroups.items.size should equal(marketGroups.totalCount)

			// Check if all market types present.
			allItemTypes.size should equal(marketType.totalCount)
		}
	}

	it should "get market prices" in {
		val prices = for(
			r <- Root.fetch();
			marketPrices <- r.marketPrices.follow(auth)
		) yield marketPrices

		whenReady(prices) { p =>
			// Check if indeed non-paginated and all items are present.
			p.totalCount should equal(p.items.size)

			p.items.foreach { it =>
				it.adjustedPrice.isNaN should be (false)
				it.averagePrice.isNaN should be (false)
				it.`type`.id_str should equal(it.`type`.id.toString)
			}
		}
	}
}
