package feather.crest.models

import com.typesafe.scalalogging.LazyLogging
import dispatch.StatusCode
import feather.crest.api.{CrestLinkParams, AuthedAsyncIterable, CrestLink}
import feather.crest.api.CrestLink.CrestProtocol._

import scala.concurrent.{Future, ExecutionContext}

object MarketOrders {

	/**
	 * An unimplemented location crestlink.
	 */
	case class Location(
		id_str: String,
		href: String,
		id: Int,
		name: String
	)

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
		location: MarketOrders.Location,
		duration: Int,
		minVolume_str: String,
		volumeEntered_str: String,
		`type`: IdNamedCrestLink[ItemType],
		id: Long,
		id_str: String
	)

}

case class MarketOrders(totalCount_str: String,
	items: List[MarketOrders.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int,
	next: Option[CrestLink[MarketOrders]],
	previous: Option[CrestLink[MarketOrders]]
)
	extends AuthedAsyncIterable[MarketOrders] with LazyLogging {
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
		if( next.isDefined ) logger.info(s"Market order has next: $next")
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
		date: String
	)

	/**
	 * Get the market history for the given type id.
	 *
	 * A Failure means that the request has not been completed (and may be retried).
	 * A Success(None) means that there was no history for the item.
	 * Any other result simply has a history for an item.
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

case class MarketHistory(totalCount_str: String,
	items: List[MarketHistory.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)


object MarketTypes {

	case class Type(
		id_str: String,
		override val href: String,
		id: Int,
		name: String,
		icon: Link
	) extends CrestLink[ItemType](href)

	case class Items(
		marketGroup: IdCrestLink[MarketGroup],
		`type`: Type
	)

}

case class MarketTypes(
	totalCount_str: String,
	pageCount: Int,
	items: List[MarketTypes.Items],
	totalCount: Int,
	pageCount_str: String,
	next: Option[CrestLink[MarketTypes]],
	previous: Option[CrestLink[MarketTypes]]
) extends AuthedAsyncIterable[MarketTypes]


case class MarketGroups(
	totalCount_str: String,
	items: List[MarketGroup],
	pageCount: Double,
	pageCount_str: String,
	totalCount: Double
)

case class MarketGroup(
	parentGroup: CrestLink[MarketGroup],
	override val href: String, // Link to itself
	name: String,
	// This link already has a parameter attached, use CrestLinkParams to prevent custom params.
	types: UncompletedCrestLink,
	description: String
) extends CrestLink[MarketGroup](href) {
	def typesLink = new CrestLinkParams[MarketTypes](types.href, Map.empty)
}

object MarketPrices {

	case class Item(
		adjustedPrice: Double,
		averagePrice: Double,
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

