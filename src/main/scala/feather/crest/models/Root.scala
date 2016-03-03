package feather.crest.models

import javax.management.remote.rmi._RMIConnection_Stub

import feather.crest.api.{CrestCollection, CrestLink}
import feather.crest.api.CrestLink.CrestProtocol._

import scala.concurrent.{Future, ExecutionContext}

object Root {
	/**
	 * Fetch a Root object from the default CREST endpoint.
	 *
	 * The user can construct a custom root object from another endpoint by constructing
	 * ```
	 * CrestLink[Root](customEndpoint)
	 * ```
	 */
	def fetch(auth: Option[String] = None)(implicit ec: ExecutionContext): Future[Root] = {
		// The only "static" CREST URL.
		val endpoint = "https://crest-tq.eveonline.com/"
		CrestLink[Root](endpoint).follow(auth)
	}

	case class Motd(dust: Link,
		eve: Link,
		server: Link
	)

	case class UserCounts(dust: Int,
		dust_str: String,
		eve: Int,
		eve_str: String
	)

	case class Industry(facilities: CrestLink[IndustryFacilities],
		systems: CrestLink[IndustrySystems]
	)

	case class Clients(dust: UnImplementedCrestLink,
		eve: UnImplementedCrestLink
	)

	case class Sovereignty(
		campaigns: CrestLink[Campaigns],
		structures: CrestLink[Structures]
	)

}

case class Root(
	crestEndpoint: CrestLink[Root],
	corporationRoles: UnImplementedCrestLink,
	itemGroups: ItemGroups,
	channels: UnImplementedCrestLink,
	corporations: UnImplementedCrestLink,
	alliances: Alliances,
	itemTypes: ItemTypes,
	decode: CrestLink[Decode],
	battleTheatres: UnImplementedCrestLink,
	marketPrices: CrestLink[MarketPrices],
	itemCategories: CrestLink[ItemCategories],
	regions: CrestLink[Regions],
	marketGroups: CrestLink[MarketGroups],
	systems: CrestLink[Collection[SolarSystem]], // not paginated
	sovereignty: Root.Sovereignty,
	tournaments: CrestLink[Tournaments],
	map: UnImplementedCrestLink,
	wars: Wars,
	incursions: TodoCrestLink,
	authEndpoint: Link,
	industry: Root.Industry,
	clients: Root.Clients,
	time: UnImplementedCrestLink,
	marketTypes: MarketTypes
)

