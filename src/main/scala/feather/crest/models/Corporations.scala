package feather.crest.models

import feather.crest.api.{CrestLink, AuthedAsyncIterable}
import CrestLink.CrestProtocol._

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
) extends AuthedAsyncIterable[Alliances]

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
)

case class Tournaments(
	totalCount_str: String,
	items: List[NamedCrestLink[Tournament]],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

case class Wars(
	totalCount_str: String,
	pageCount: Int,
	items: List[IdCrestLink[War]],
	next: Option[CrestLink[Wars]],
	prev: Option[CrestLink[Wars]],
	totalCount: Int,
	pageCount_str: String
)

object War {

	case class Ally(
		name: String,
		override val href: String,
		id_str: String,
		icon: Link,
		id: Int
	) extends CrestLink[Corporation](href)

	case class Belligerent(
		shipsKilled: Int,
		shipsKilled_str: String,
		name: String,
		override val href: String,
		id_str: String,
		icon: Link,
		id: Int,
		iskKilled: Double
	) extends CrestLink[Corporation](href)

}

case class War(
	timeFinished: String,
	openForAllies: Boolean,
	allies: Option[List[War.Ally]],
	timeStarted: String,
	allyCount: Int,
	timeDeclared: String,
	aggressor: War.Belligerent,
	mutual: Boolean,
	allyCount_str: String,
	killmails: String,
	id_str: String,
	defender: War.Belligerent,
	id: Double
) {
	def killMailLink: CrestLink[KillMails] = {
		CrestLink[KillMails](killmails)
	}
}
