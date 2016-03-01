package feather.crest.models

import java.net.{URI, URL}

import com.typesafe.scalalogging.LazyLogging
import dispatch.StatusCode
import feather.crest.api.{CrestCollection, CrestLinkParams, CrestLink}
import feather.crest.api.CrestLink.CrestProtocol._
import feather.crest.util.Util

import scala.concurrent.{Future, ExecutionContext}

object MarketOrders {

	/**
	 * An unimplemented location crestlink.
	 *
	 * @todo Wait for implementation
	 */
	case class Location(
		id_str: String,
		href: String,
		id: Int,
		name: String
	)

	/**
	 * A market order.
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
		location: MarketOrders.Location,
		duration: Int,
		minVolume_str: String,
		volumeEntered_str: String,
		`type`: IdNamedCrestLink[ItemType],
		id: Long,
		id_str: String
	)

}

/**
 * Market orders for an item.
 *
 * The item was specified in obtaining this class.
 * Experimentally we have checked if pagination occurs,
 * but that does not seem the case.
 * Thus we have removed the pagination fields.
  *
  * @see The associated ModelOnce specification test.
 */
case class MarketOrders(totalCount_str: String,
	items: List[MarketOrders.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

object MarketHistory {

	case class Item(volume_str: String,
		orderCount: Long,
		lowPrice: Double,
		highPrice: Double,
		avgPrice: Double,
		volume: Long,
		orderCount_str: String,
		date: String
	)

	/**
	 * Get the market history for the given type id.
	 *
	 * A Failure means that the request has not been completed (and may be retried).
	 * A Success(None) means that there was no history for the item.
	 * Any other result simply has a history for an item.
	  *
	  * @param regionLink A CrestLink to the region for which to get market history.
	 * @param typeLink A CrestLink to the itemtype for which to get market history.
	 * @param auth The authentication code.
	 * @return
	 */
	def fetch(regionLink: CrestLink[Region], typeLink: CrestLink[ItemType])
			(auth: Option[String], retries: Int = 1)
			(implicit ec: ExecutionContext): Future[Option[MarketHistory]] = {
		val region = regionLink.href
		val marketIdRegex = """.*/(\d+)/.*""".r
		val marketId = marketIdRegex.findFirstMatchIn(region).map(_.group(1))

		// Get the itemType link.
		val itemType = typeLink.href
		val itemTypeIdRegex = """.*/(\d+)/""".r
		val typeId = itemTypeIdRegex.findFirstMatchIn(itemType).map(_.group(1))

		(for (
			mId <- marketId;
			tId <- typeId
		) yield {
				val url = s"https://crest-tq.eveonline.com/market/$mId/types/$tId/history/"
				CrestLink[MarketHistory](url).follow(auth).map(Some.apply).recover {
					case StatusCode(404) ⇒
						// If the given typeID does not have a history page
						// (because it's not on the market), return a successful None.
						None
				}
			}) getOrElse {
			// Something went wrong in the regexes. Fail gracefully.
			Future.failed(throw new NoSuchElementException("Something went wrong extracting the market ID or type ID."))
		}
	}
}

/**
 * Market history is only available through a direct request.
 *
 * There is no CREST link to the Market history.
 */
case class MarketHistory(totalCount_str: String,
	items: List[MarketHistory.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)


object MarketTypesPage {
	case class Type(
		id_str: String,
		override val href: String,
		id: Int,
		name: String,
		icon: Link
	) extends CrestLink[ItemType](href)
}

case class MarketTypesPage (
	marketGroup: IdCrestLink[MarketGroup],
	`type`: MarketTypesPage.Type
)

/**
  * There is only one page of MarketGroups, so no pagination required.
  */
case class MarketGroups(
	totalCount_str: String,
	items: List[MarketGroup],
	pageCount: Double,
	pageCount_str: String,
	totalCount: Double
)

/**
 * A group of market items.
 *
 * @param parentGroup
 * @param href The Crest URL to the Crest instance.
 * @param name
 * @param types A link to the market types in this group.
 *              Note that this link already has a parameter attached, so custom parameter will break the link.
 * @param description
 * @todo Figure out the types field with bound parameters.
 */
case class MarketGroup(
	parentGroup: Option[CrestLink[MarketGroup]],
	override val href: String, // Link to itself
	name: String,
	types: MarketTypes,
	description: String
) extends CrestLink[MarketGroup](href)

object MarketPrices {
	case class Item(
		adjustedPrice: Option[Double],
		averagePrice: Option[Double],
		`type`: IdNamedCrestLink[ItemType]
	)

}

case class MarketPrices(
	totalCount_str: String,
	items: List[MarketPrices.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

