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

object IndustryFacilities {

	case class ID(id: Int, id_str: String)

	case class Item(
		facilityID: Int,
		solarSystem: ID,
		name: String,
		region: ID,
		tax: Option[Double],
		facilityID_str: String,
		owner: ID,
		`type`: ID
	)

}

/**
 * The facilities provided for industry.
 */
case class IndustryFacilities(
	totalCount_str: String,
	items: List[IndustryFacilities.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)


object IndustrySystems {

	case class SystemCostIndex(
		costIndex: Double,
		activityID: Int,
		activityID_str: String,
		activityName: String
	)

	case class Item(
		systemCostIndices: List[SystemCostIndex],
		solarSystem: IdNamedCrestLink[SolarSystem]
	)

}

/**
 * The systems for industry.
 */
case class IndustrySystems(
	totalCount_str: String,
	items: List[IndustrySystems.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)


