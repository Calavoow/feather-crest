package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.api.CrestLink.CrestProtocol._
import feather.crest.api.TwitterConverters._
import feather.crest.models._
import org.scalatest.concurrent.AsyncAssertions.{PatienceConfig, Waiter, dismissals}
import org.scalatest.{FlatSpec, Matchers}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

/**
 * Slow tests that should be checked once in a while.
 */
class ModelOnceSpec extends FlatSpec with Matchers with LazyLogging {
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

	it should "All market orders should not contain a second page" in {
		implicit val patienceConfig = PatienceConfig(timeout = 20 minutes)
		// As usual get the Root first.
		val root = Root.fetch(None)

		val theForge: Future[Region] = async {
			// Note: no Futures! But everything outside async will stil be asynchronous.
			val aRoot: Root = await(root)
			val regions: Regions = await(aRoot.regions.follow(auth))
			// Note that I use {{.get}} here, which could throw an exception,
			// but simplifies this example.
			val forge: Region = await(regions.items.find(_.name == "The Forge").get
				.follow(auth)
			)

			/**
			 * Oops, from the type of {{theForge.marketSellLink}}
			 * we see that we need an CrestLink[ItemType],
			 * so lets handle that in _parallel_ (below).
			 */
			forge
		}

		val futItemTypes = async {
			val aRoot: Root = await(root)
			// Lets fetch the Hammerhead II
			val itemTypes: ItemTypes = await(aRoot.itemTypes.follow(auth))
			await(itemTypes.paramsIterator(auth).map(_.items).reduceLeft(_ ++ _))
		}

		def invariantOrder(marketOrders: MarketOrders): Unit = {
			// Check if no pagination happens.
			marketOrders.items.size should equal(marketOrders.totalCount)
		}

		val waiter = new Waiter
		// Now we put everything together and get all buy and sell orders.
		async {
			val aTheForge: Region = await(theForge)
			val itemTypes = await(futItemTypes)
			println(itemTypes.size)

			for(itemTypeGroup <- itemTypes.grouped(25)) {
				logger.debug(s"${itemTypeGroup.head}")
				val out = for(itemType <- itemTypeGroup) yield {
					async {
						val buyOrder = await(aTheForge.marketBuyLink(itemType).follow(auth))
						val sellOrder = await(aTheForge.marketSellLink(itemType).follow(auth))
						waiter {
							invariantOrder(buyOrder)
							invariantOrder(sellOrder)
						}
						waiter.dismiss()
					}
				}
				// Block after every group, so that not too many requests are sent at once.
				Await.ready(Future.sequence(out), 10 seconds)
				logger.debug("Next group")
			}
		}
		// At least 10k of the buy and sell orders must have no second page, to check this property.
		waiter.await(dismissals(2000))
	}
}
