package feather.crest.models

import feather.crest.api.{AuthedAsyncIterable, CrestLink}
import feather.crest.api.CrestLink.CrestProtocol._
import spray.json.JsonReader


object Tournament {

	/**
	 * Todo: Implement Entry link to team.
	 */
	case class Entry(
		teamStats: TodoCrestLink,
		href: String,
		name: String
	)

}

case class Tournament(
	series: TodoCrestLink,
	`type`: String,
	name: String,
	entries: List[Tournament.Entry]
)


case class KillMails(
	totalCount_str: String,
	items: List[IdCrestLink[KillMail]],
	next: Option[CrestLink[KillMails]],
	prev: Option[CrestLink[KillMails]],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
) extends AuthedAsyncIterable[KillMails]


object KillMail {
	case class ImageLink[T: JsonReader](
		id_str: String,
		override val href: String,
		id: Double,
		name: String,
		icon: Link
	) extends CrestLink[T](href)

	case class Attacker(
		shipType: ImageLink[ItemType],
		corporation: ImageLink[Corporation],
		character: ImageLink[Character],
		damageDone_str: String,
		weaponType: ImageLink[ItemType],
		finalBlow: Boolean,
		securityStatus: Double,
		damageDone: Int
	)

	case class Victim(
		damageTaken: Int,
		corporation: ImageLink[Corporation],
		damageTaken_str: String,
		character: ImageLink[Character],
		shipType: ImageLink[ItemType],
		items: List[KillMail.Item]
	)

	case class Item(
		singleton: Int,
		itemType: ImageLink[ItemType],
		flag: Int,
		flag_str: String,
		singleton_str: String,
		quantityDestroyed_str: Option[String],
		quantityDestroyed: Option[Int],
		quantityDropped_str: Option[String],
		quantityDropped: Option[Int]
	)
}

case class KillMail(
	solarSystem: IdNamedCrestLink[SolarSystem],
	killID: Int,
	killTime: String,
	attackers: List[KillMail.Attacker],
	attackerCount: Int,
	victim: KillMail.Victim,
	killID_str: String,
	attackerCount_str: String,
	war: Option[IdCrestLink[War]] //Guessing that not every killmail has a war.
)

