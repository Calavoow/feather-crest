package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import dispatch.StatusCode
import spray.json.JsonFormat

import scala.concurrent.{ExecutionContext, Future}

object Models extends LazyLogging {
	import CrestLink.CrestProtocol._

	/**
	 * A CrestContainer is a class that contains links to followup pages and the information on the current crest page.
	 */
	sealed trait CrestContainer

	case class UnImplementedCrestLink(href: String) extends CrestContainer

	/**
	 * To follow this crest link some construction is required.
	 *
	 * See the class methods to construct a normal CrestLink
	 * @param href The link to follow.
	 */
	case class UncompletedCrestLink(href: String) extends CrestContainer

	object NamedCrestLink{
		implicit class NamedCrestLink2CrestLink[T: JsonFormat](nlink: NamedCrestLink[T]) {
			def toCrestLink: CrestLink[T] = {
				nlink.link
			}
		}
	}
	case class NamedCrestLink[T: JsonFormat](href: String, name: String) {
		lazy val link = CrestLink[T](href)
	}

	case class UnImplementedNamedCrestLink(href: String, name: String) extends CrestContainer

	object Root {
		def fetch(auth: Option[String])(implicit ec: ExecutionContext): Future[Root] = {
			import CrestLink.CrestProtocol._
			// The only "static" CREST URL.
			val endpoint = "https://crest-tq.eveonline.com/"
			CrestLink[Root](endpoint).follow(auth)
		}

		case class Motd(dust: UnImplementedCrestLink,
		                eve: UnImplementedCrestLink,
		                server: UnImplementedCrestLink)

		case class UserCounts(dust: Int,
		                      dust_str: String,
		                      eve: Int,
		                      eve_str: String)

		case class Industry(facilities: UnImplementedCrestLink,
		                    systems: UnImplementedCrestLink)

		case class Clients(dust: UnImplementedCrestLink,
		                   eve: UnImplementedCrestLink)

	}

	case class Root(crestEndpoint: UnImplementedCrestLink,
	                corporationRoles: UnImplementedCrestLink,
	                itemGroups: UnImplementedCrestLink,
	                channels: UnImplementedCrestLink,
	                corporations: UnImplementedCrestLink,
	                alliances: UnImplementedCrestLink,
	                itemTypes: CrestLink[ItemTypes],
	                decode: UnImplementedCrestLink,
	                battleTheatres: UnImplementedCrestLink,
	                marketPrices: UnImplementedCrestLink,
	                itemCategories: UnImplementedCrestLink,
	                regions: CrestLink[Regions],
	                marketGroups: UnImplementedCrestLink,
	                tournaments: UnImplementedCrestLink,
	                map: UnImplementedCrestLink,
	                wars: UnImplementedCrestLink,
	                incursions: UnImplementedCrestLink,
	                authEndpoint: UnImplementedCrestLink,
	                industry: Root.Industry,
	                clients: Root.Clients,
	                time: UnImplementedCrestLink,
	                marketTypes: UnImplementedCrestLink) extends CrestContainer

	case class Regions(totalCount_str: String,
	                   items: List[NamedCrestLink[Region]],
	                   pageCount: Int,
	                   pageCount_str: String,
	                   totalCount: Int) extends CrestContainer

	case class Region(description: String,
	                  marketBuyOrders: UncompletedCrestLink,
	                  name: String,
	                  constellations: List[UnImplementedCrestLink],
	                  marketSellOrders: UncompletedCrestLink) extends CrestContainer {
		def marketBuyLink(itemType: CrestLink[ItemType]) : CrestLinkParams[MarketOrders] = {
			new CrestLinkParams[MarketOrders](marketBuyOrders.href, Map("type" → itemType.href))
		}

		def marketSellLink(itemType: CrestLink[ItemType]): CrestLinkParams[MarketOrders] = {
			new CrestLinkParams[MarketOrders](marketSellOrders.href, Map("type" → itemType.href))
		}
	}

	case class ItemTypes(totalCount_str: String,
	                     pageCount: Int,
	                     items: List[NamedCrestLink[ItemType]],
	                     next: Option[CrestLink[ItemTypes]],
	                     totalCount: Int,
	                     pageCount_str: String,
	                     previous: Option[CrestLink[ItemTypes]])
		extends CrestContainer with AuthedAsyncIterable[ItemTypes] {
		def authedIterator(auth: Option[String], retries: Int = 1)
		                  (implicit ec: ExecutionContext) = {
			if(next.isDefined) logger.info(s"Itemtypes has next: $next")
			// Cannot partially apply function, because of the implicit execution context.
			this.paramsIterator(Map.empty)(auth, retries)
		}
	}

	/**
	 * An ItemType
	 */
	case class ItemType(name: String, description: String) extends CrestContainer

	object MarketOrders {

		/**
		 * A reference to an item type.
		 * @param id_str String of the id
		 * @param href The link is not implemented yet in the EVE CREST.
		 * @param id The id
		 * @param name The name of the itemtype.
		 */
		case class Reference(id_str: String,
		                     href: String,
		                     id: Int,
		                     name: String)

		/**
		 *
		 * @param href The link has not been implemented yet in EVE CREST.
		 */
		case class Item(volume_str: String,
		                buy: Boolean,
		                issued: String,
		                price: Double,
		                volumeEntered: Long,
		                minVolume: Long,
		                volume: Long,
		                range: String,
		                href: String,
		                duration_str: String,
		                location: Reference,
		                duration: Int,
		                minVolume_str: String,
		                volumeEntered_str: String,
		                `type`: Reference,
		                id: Long,
		                id_str: String)

	}

	case class MarketOrders(totalCount_str: String,
	                        items: List[MarketOrders.Item],
	                        pageCount: Int,
	                        pageCount_str: String,
	                        totalCount: Int,
	                        next: Option[CrestLink[MarketOrders]],
	                        previous: Option[CrestLink[MarketOrders]])
		extends CrestContainer with AuthedAsyncIterable[MarketOrders] {
		/**
		 * Construct an asynchonous iterator through the market orders.
		 *
		 * A parameter itemType is required to iterate through the market orders.
		 *
		 * TODO: Check if this is the case
		 * @param itemType A CREST link to the itemtype for which market orders should be retrieved.
		 * @return An asyncIterator through the market orders of the given itemType.
		 */
		def authedIterator(itemType: CrestLink[ItemType])
		                  (auth: Option[String], retries: Int = 1)
		                  (implicit ec: ExecutionContext) = {
			if(next.isDefined) logger.info(s"Market order has next: $next")
			// Cannot partially apply function, because of the implicit execution context.
			this.paramsIterator(Map("type" → itemType.href))(auth, retries)
		}
	}

	object MarketHistory {
		case class Item(volume_str: String,
			                orderCount: Long,
			                lowPrice: Double,
			                highPrice: Double,
			                avgPrice: Double,
			                volume: Long,
			                orderCount_str: String,
			                date: String)

		/**
		 * Get the market history for the given type id.
		 *
		 * A Failure means that the request has not been completed (and may be retried).
		 * A Success(None) means that there was no history for the item.
		 * Any other result simply has a history for an item.
		 * @param marketID The market ID.
		 * @param typeID The type ID.
		 * @param auth The authentication code.
		 * @return
		 */
		def fetch(marketID: Int, typeID: Int, auth: Option[String])
		         (implicit ec: ExecutionContext): Future[Option[MarketHistory]] = {
			import CrestLink.CrestProtocol._
			val url = s"https://crest-tq.eveonline.com/market/$marketID/types/$typeID/history/"
			CrestLink[MarketHistory](url).follow(auth).map(Some.apply).recover{
				case StatusCode(404) ⇒
					// If the given typeID does not have a history page (maybe because it's not on the market), return None.
					None
			}
		}
	}

	case class MarketHistory(totalCount_str: String,
		                         items: List[MarketHistory.Item],
		                         pageCount: Int,
		                         pageCount_str: String,
		                         totalCount: Int) extends CrestContainer


}
