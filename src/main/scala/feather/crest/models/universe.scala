package feather.crest.models

import feather.crest.api.{CrestLink, CrestLinkParams}
import feather.crest.api.CrestLink.CrestProtocol._

/**
 * A position in space.
 */
case class Position(
	y: Long,
	x: Long,
	z: Long
)

/**
 * Regions of the universe, not paginated.
 */
case class Regions(totalCount_str: String,
	items: List[IdNamedCrestLink[Region]],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

case class Region(description: String,
	marketBuyOrders: UncompletedCrestLink,
	name: String,
	constellations: List[CrestLink[Constellation]],
	marketSellOrders: UncompletedCrestLink
) {
	def marketBuyLink(itemType: CrestLink[ItemType]): CrestLinkParams[MarketOrders] = {
		new CrestLinkParams[MarketOrders](marketBuyOrders.href, Map("type" → itemType.href))
	}

	def marketSellLink(itemType: CrestLink[ItemType]): CrestLinkParams[MarketOrders] = {
		new CrestLinkParams[MarketOrders](marketSellOrders.href, Map("type" → itemType.href))
	}
}

case class SolarSystem(
	stats: UnImplementedCrestLink,
	name: String,
	securityStatus: Double,
	securityClass: Option[String],
	href: String, // A link to itself?
	planets: List[CrestLink[Planet]],
	position: Position,
	sovereignty: Option[IdNamedCrestLink[Alliance]],
	constellation: CrestLink[Constellation]
)

case class Constellation(
	position: Position,
	region: CrestLink[Region],
	systems: List[CrestLink[SolarSystem]],
	name: String
)


case class Planet(
	position: Position,
	`type`: CrestLink[ItemType],
	system: NamedCrestLink[SolarSystem],
	name: String
)

