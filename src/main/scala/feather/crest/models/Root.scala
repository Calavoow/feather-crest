package feather.crest.models

import feather.crest.api.CrestLink
import feather.crest.api.CrestLink.CrestProtocol._

import scala.concurrent.{Future, ExecutionContext}

object Root {
	def fetch(auth: Option[String])(implicit ec: ExecutionContext): Future[Root] = {
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

}

case class Root(crestEndpoint: CrestLink[Root],
	corporationRoles: UnImplementedCrestLink,
	itemGroups: CrestLink[ItemGroups],
	channels: UnImplementedCrestLink,
	corporations: UnImplementedCrestLink,
	alliances: CrestLink[Alliances],
	itemTypes: CrestLink[ItemTypes],
	decode: CrestLink[Decode],
	battleTheatres: UnImplementedCrestLink,
	marketPrices: CrestLink[MarketPrices],
	itemCategories: CrestLink[ItemCategories],
	regions: CrestLink[Regions],
	marketGroups: CrestLink[MarketGroups],
	tournaments: CrestLink[Tournaments],
	map: UnImplementedCrestLink,
	wars: CrestLink[Wars],
	incursions: TodoCrestLink,
	authEndpoint: Link,
	industry: Root.Industry,
	clients: Root.Clients,
	time: UnImplementedCrestLink,
	marketTypes: CrestLink[MarketTypes]
)

