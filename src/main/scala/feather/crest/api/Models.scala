package feather.crest.api

import java.util.NoSuchElementException

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

	/**
	 * To follow this crest link some construction is required.
	 *
	 * See the class methods to construct a normal CrestLink
	 */
	case class UncompletedCrestLink(href: String) extends CrestContainer

	/**
	 * A CrestLink with a name field.
	 * @param href The Crest URL to the next link
	 * @param name The name field.
	 * @tparam T The type of CrestContainer to construct.
	 */
	case class NamedCrestLink[T: JsonFormat](override val href : String, name: String) extends CrestLink[T](href)
	case class IdNamedCrestLink[T: JsonFormat](
		id_str: String,
		override val href : String,
		id: Int,
		name: String
	) extends CrestLink[T](href)

	/**
	 * A CrestLink which has not been implemented by CCP.
	 */
	case class UnImplementedCrestLink(href: String) extends CrestContainer
	/**
	 * A CrestLink with a name that has not been implemented by CCP.
	 */
	case class UnImplementedNamedCrestLink(href: String, name: String) extends CrestContainer

	/**
	 * A CrestLink which has not been implemented by Feather-Crest.
	 */
	case class TodoCrestLink(href: String) extends CrestContainer
	/**
	 * A CrestLink which has not been implemented by Feather-Crest.
	 */
	case class TodoNamedCrestLink(href: String, name: String) extends CrestContainer

	/**
	 * A simple hyper-reference
	 */
	case class Link(href: String) extends CrestContainer

	/**
	 * A picture
	 */
	case class Picture (
		`32x32`  : Link,
		`64x64`  : Link,
		`128x128`: Link,
		`256x256`: Link
	)

	/**
	 * An Eve Corporation
	 */
	case class Corporation(
		name: String,
		isNPC: Boolean,
		href: String,
		id_str: String,
		logo: Picture,
		id: Int
	)

	/**
	 * A position in space.
	 */
	case class Position(
		y: Long,
		x: Long,
		z: Long
	)

	object Root {
		def fetch(auth: Option[String])(implicit ec: ExecutionContext): Future[Root] = {
			// The only "static" CREST URL.
			val endpoint = "https://crest-tq.eveonline.com/"
			CrestLink[Root](endpoint).follow(auth)
		}

		case class Motd(dust: Link,
		                eve: Link,
		                server: Link)

		case class UserCounts(dust: Int,
		                      dust_str: String,
		                      eve: Int,
		                      eve_str: String)

		case class Industry(facilities: CrestLink[IndustryFacilities],
		                    systems: CrestLink[IndustrySystems])

		case class Clients(dust: UnImplementedCrestLink,
		                   eve: UnImplementedCrestLink)

	}

	case class Root(crestEndpoint: CrestLink[Root],
	                corporationRoles: UnImplementedCrestLink,
	                itemGroups: CrestLink[ItemGroups],
	                channels: UnImplementedCrestLink,
	                corporations: UnImplementedCrestLink,
	                alliances: UnImplementedCrestLink,
	                itemTypes: CrestLink[ItemTypes],
	                decode: CrestLink[Decode],
	                battleTheatres: UnImplementedCrestLink,
	                marketPrices: CrestLink[MarketPrices],
	                itemCategories: CrestLink[ItemCategories],
	                regions: CrestLink[Regions],
	                marketGroups: TodoCrestLink,
	                tournaments: TodoCrestLink,
	                map: UnImplementedCrestLink,
	                wars: TodoCrestLink,
	                incursions: TodoCrestLink,
	                authEndpoint: Link,
	                industry: Root.Industry,
	                clients: Root.Clients,
	                time: UnImplementedCrestLink,
	                marketTypes: TodoCrestLink) extends CrestContainer

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
		                location: IdNamedCrestLink[],
		                duration: Int,
		                minVolume_str: String,
		                volumeEntered_str: String,
		                `type`: IdNamedCrestLink[ItemType],
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

			(for(
				mId <- marketId;
				tId <- typeId
			) yield {
				val url = s"https://crest-tq.eveonline.com/market/$mId/types/$tId/history/"
				CrestLink[MarketHistory](url).follow(auth).map(Some.apply).recover{
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
		                         totalCount: Int) extends CrestContainer



	case class ItemCategories(totalCount_str: String,
		                         items: List[NamedCrestLink[ItemCategory]],
		                         pageCount: Int,
		                         pageCount_str: String,
		                         totalCount: Int) extends CrestContainer

	case class ItemCategory(
		name: String,
		groups: List[NamedCrestLink[ItemGroup]],
		published: Boolean
	) extends CrestContainer

	case class ItemGroups(
		totalCount_str: String,
		pageCount: Int,
		items: List[NamedCrestLink[ItemGroup]],
		next: Option[CrestLink[ItemGroups]],
		previous: Option[CrestLink[ItemGroups]],
		totalCount: Int,
		pageCount_str: String
	) extends CrestContainer with AuthedAsyncIterable[ItemGroups]

	case class ItemGroup(
		category: CrestLink[ItemCategory],
		description: String,
		name: String,
		types: List[NamedCrestLink[ItemType]],
		published: Boolean
	) extends CrestContainer

	object Alliances {
		case class AllianceLink(
		   id_str: String,
		   shortName: String,
		   override val href: String,
		   id: Double,
		   name: String
		) extends CrestLink[Alliance](href)

	}
	case class Alliances(
		totalCount_str: String,
		pageCount: Int,
		items: List[Alliances.AllianceLink],
		next: Option[CrestLink[Alliances]],
		totalCount: Int,
		pageCount_str: String,
		previous: Option[CrestLink[Alliances]]
	) extends CrestContainer with AuthedAsyncIterable[Alliances]

	object Alliance {
		case class Character(
			name: String,
			isNPC: Boolean,
			href: String,
			capsuleer: UnImplementedCrestLink,
			portrait: Picture,
			id: Int,
			id_str: String
		)
	}
	case class Alliance(
		startDate: String,
		corporationsCount: Int,
		description: String,
		executorCorporation: Corporation,
		corporationsCount_str: String,
		deleted: Boolean,
		creatorCorporation: Corporation,
		url: String,
		id_str: String,
		creatorCharacter: Corporation,
		corporations: List[Corporation],
		shortName: String,
		id: Int,
		name: String
	) extends CrestContainer


	case class Decode(character: CrestLink[Character]) extends CrestContainer

	object Character {

		/**
		 * The bloodline of a character.
		 *
		 * @param href Unimplemented
		 */
		case class BloodLine (
			href: String,
			id: Int,
			id_str: String
		)

		/**
		 * The race of the character.
		 *
		 * @param href Unimplemented
		 */
		case class Race (
			href: String,
			id: Int,
			id_str: String
		)
	}

	case class Character(
		standings: TodoCrestLink,
		bloodLine: Character.BloodLine,
		gender_str: String,
		`private`: UnImplementedCrestLink,
		channels: UnImplementedCrestLink,
		// href: String, // Refers to this page? Left out because of 23-arity
		accounts: UnImplementedCrestLink,
		portrait: Picture,
		id: Int,
		blocked: TodoCrestLink,
		statistics: TodoCrestLink,
		contacts: TodoCrestLink,
		corporation: Corporation,
		id_str: String,
		mail: TodoCrestLink,
		capsuleer: UnImplementedCrestLink,
		vivox: UnImplementedCrestLink,
		description: String,
		notifications: TodoCrestLink,
		name: String,
		gender: Int,
		race: Character.Race,
		deposit: UnImplementedCrestLink
	) extends CrestContainer

	object MarketPrices {
		case class Item (
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
	) extends CrestContainer

	object IndustryFacilities {
		case class ID(id: Int, id_str: String)
		case class Item(
			facilityID : Int,
			solarSystem : ID,
			name : String,
			region : ID,
			tax : Double,
			facilityID_str : String,
			owner: ID,
			`type` : ID
		)
	}

	case class IndustryFacilities (
		totalCount_str: String,
		items: List[IndustryFacilities.Item],
		pageCount: Int,
		pageCount_str: String,
		totalCount: Int
	) extends CrestContainer


	object IndustrySystems {
		case class SystemCostIndex (
			costIndex: Double,
			activityID: Int,
			activityID_str: String,
			activityName: String
		)

		case class Item (
			systemCostIndices: List[SystemCostIndex],
			solarSystem: IdNamedCrestLink[SolarSystem]
		)
	}
	case class IndustrySystems (
		totalCount_str: String,
		items: List[IndustrySystems.Item],
		pageCount: Int,
		pageCount_str: String,
		totalCount: Int
	) extends CrestContainer

	case class SolarSystem(
		stats: UnImplementedCrestLink,
		name: String,
		securityStatus: Double,
		securityClass: String,
		href: String, // A link to itself?
		planets: List[CrestLink[Planet]],
		position: Position,
		sovereignty: IdNamedCrestLink[Alliance],
		constellation: CrestLink[Constellation]
	) extends CrestContainer

	case class Constellation(
		position: Position,
		region: CrestLink[Region],
		systems: List[CrestLink[SolarSystem]],
		name: String
	) extends CrestContainer


	case class Planet(
		position: Position,
		`type`: CrestLink[ItemType],
		system: NamedCrestLink[SolarSystem],
		name: String
	)
}
