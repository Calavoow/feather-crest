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

import feather.crest.api.{CrestCollection, CrestLink}
import feather.crest.api.CrestLink.CrestProtocol._

import feather.crest.models.{Character => CharacterPage}

/**
 * An Eve Corporation
 *
 * @param href Link to this corporation's page. Currently unimplemented.
 */
case class Corporation(
	name: String,
	isNPC: Boolean,
	href: String,
	id_str: String,
	logo: Picture,
	id: Int
)

case class AllianceLink(
	id_str: Option[String],
	shortName: String,
	override val href: String,
	id: Double,
	name: String
) extends CrestLink[Alliance](href)

//case class Alliances(
//	totalCount_str: String,
//	pageCount: Int,
//	items: List[Alliances.AllianceLink],
//	next: Option[CrestLink[Alliances]],
//	totalCount: Int,
//	pageCount_str: String,
//	previous: Option[CrestLink[Alliances]]
//) extends AuthedAsyncIterable[Alliances]

object Alliance {

	case class Character(
		name: String,
		isNPC: Boolean,
		override val href: String,
		capsuleer: UnImplementedCrestLink,
		portrait: Picture,
		id: Int,
		id_str: String
	) extends CrestLink[CharacterPage](href)

}

/**
 * An Alliance of [[Corporation]]s
 */
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
	creatorCharacter: Alliance.Character,
	corporations: List[Corporation],
	shortName: String,
	id: Int,
	name: String
)

//case class Wars(
//	totalCount_str: String,
//	pageCount: Int,
//	items: List[IdCrestLink[War]],
//	next: Option[CrestLink[Wars]],
//	prev: Option[CrestLink[Wars]],
//	totalCount: Int,
//	pageCount_str: String
//)

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
	timeFinished: Option[String],
	openForAllies: Boolean,
	allies: Option[List[War.Ally]],
	timeStarted: Option[String],
	allyCount: Int,
	timeDeclared: String,
	aggressor: War.Belligerent,
	mutual: Boolean,
	allyCount_str: String,
	killmails: String,
	id_str: String,
	defender: War.Belligerent,
	id: Long
) {
	def killMailsLink: KillMails = {
		new KillMails(killmails)
	}
}

object Structures {
	case class Item(
		alliance: IdNamedCrestLink[Alliance],
		vulnerabilityOccupancyLevel: Option[Double],
		structureID_str: String,
		structureID: Long,
		vulnerableStartTime: Option[String],
		solarSystem: IdNamedCrestLink[SolarSystem],
		vulnerableEndTime: Option[String],
		`type`: IdNamedCrestLink[ItemTypes]
	)
//	{
//		override def toString = {
//			s"""Item(
//			   |    alliance: $alliance
//			   |    vulnerabilityOccupancyLevel: $vulnerabilityOccupancyLevel
//			   |    structureID_str: $structureID_str
//			   |    structureID: $structureID
//			   |    vulnerableStartTime: $vulnerableStartTime
//			   |    solarSystem: $solarSystem
//			   |    vulnerableEndTime: $vulnerableEndTime
//			   |    type: ${`type`}
//			   |)
//			 """.stripMargin
//		}
//	}
}

/**
 * "This lists every structure in New Eden that is contributing to sovereignty be it a TCU, IHub, or a station."
 *
 * @see https://developers.eveonline.com/blog/article/aegis-sovereignty-api-changes
 */
case class Structures (
	totalCount_str: String,
	items: List[Structures.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

object Campaigns {
	case class Attackers(
		score: Double
	)

	case class Defender(
		defender: IdNamedCrestLink[Alliance],
		score: Double
	)

	case class Score(
		score: Double,
		team: IdNamedCrestLink[Alliance]
	)

	/**
	 * A Campaign item, describing a sov campaign.
	 *
	 * @param eventType is 1,2,3 or 4.
	 *                  If 1-3 then the [[Campaigns.Item]] has [[attackers]] and [[defender]],
	 *                  if 4 then it has [[scores]].
	 * @see https://developers.eveonline.com/blog/article/aegis-sovereignty-api-changes
	 */
	case class Item(
		eventType_str: String,
		campaignID: Int,
		eventType: Int,
		sourceSolarsystem: IdNamedCrestLink[SolarSystem],
		attackers: Option[Campaigns.Attackers],
		campaignID_str: String,
		sourceItemID: Long,
		startTime: String,
		sourceItemID_str: String,
		defender: Option[Campaigns.Defender],
		constellation: IdNamedCrestLink[Constellation],
		scores: Option[List[Score]]
	)
}

/**
 * A command node campaign will occur when a sov structure has been successfully contested.
 */
case class Campaigns (
	totalCount_str: String,
	items: List[Campaigns.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

