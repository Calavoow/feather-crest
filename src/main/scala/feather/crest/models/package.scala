package feather.crest

package object models {
	type ItemTypes = PaginatedResource[NamedCrestLink[ItemType]]
	type ItemGroups = PaginatedResource[NamedCrestLink[ItemGroup]]
	type Alliances = PaginatedResource[AlliancesPage.AllianceLink]
	type MarketTypes = PaginatedResource[MarketTypesPage.Item]
	type KillMails = PaginatedResource[IdCrestLink[KillMail]]
}
