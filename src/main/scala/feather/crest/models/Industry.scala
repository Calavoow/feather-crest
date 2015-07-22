package feather.crest.models

object IndustryFacilities {

	case class ID(id: Int, id_str: String)

	case class Item(
		facilityID: Int,
		solarSystem: ID,
		name: String,
		region: ID,
		tax: Double,
		facilityID_str: String,
		owner: ID,
		`type`: ID
	)

}

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

case class IndustrySystems(
	totalCount_str: String,
	items: List[IndustrySystems.Item],
	pageCount: Int,
	pageCount_str: String,
	totalCount: Int
)

