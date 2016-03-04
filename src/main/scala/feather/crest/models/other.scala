/*
 * Feather-Crest is a library that simplifies interaction with the EVE Online API.
 * Copyright (C) 2016 Calavoow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package feather.crest.models

import feather.crest.api.CrestLink
import spray.json.JsonReader


case class Tournaments(
	totalCount_str: String,
	items: List[NamedCrestLink[Tournament]],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

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

//class KillMails extends PaginatedResource[IdCrestLink[KillMail]]

//case class KillMails(
//	totalCount_str: String,
//	items: List[IdCrestLink[KillMail]],
//	next: Option[CrestLink[KillMails]],
//	prev: Option[CrestLink[KillMails]],
//	pageCount: Int,
//	pageCount_str: String,
//	totalCount: Int
//) extends AuthedAsyncIterable[KillMails]


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

