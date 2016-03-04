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

//case class ItemTypes(totalCount_str: String,
//	pageCount: Int,
//	items: List[NamedCrestLink[ItemType]],
//	next: Option[CrestLink[ItemTypes]],
//	totalCount: Int,
//	pageCount_str: String,
//	previous: Option[CrestLink[ItemTypes]]
//) extends AuthedAsyncIterable[ItemTypes]

/**
 * An ItemType
 */
case class ItemType(name: String, description: String)


case class ItemCategories(totalCount_str: String,
	items: List[NamedCrestLink[ItemCategory]],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

case class ItemCategory(
	name: String,
	groups: List[NamedCrestLink[ItemGroup]],
	published: Boolean
)

//case class ItemGroups(
//	totalCount_str: String,
//	pageCount: Int,
//	items: List[NamedCrestLink[ItemGroup]],
//	next: Option[CrestLink[ItemGroups]],
//	previous: Option[CrestLink[ItemGroups]],
//	totalCount: Int,
//	pageCount_str: String
//) extends AuthedAsyncIterable[ItemGroups]

case class ItemGroup(
	category: CrestLink[ItemCategory],
	description: String,
	name: String,
	types: List[NamedCrestLink[ItemType]],
	published: Boolean
)

