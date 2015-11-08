package feather.crest

package object models {
	type ItemTypes = PaginatedResource[NamedCrestLink[ItemType]]
	type ItemGroups = PaginatedResource[NamedCrestLink[ItemGroup]]
	type Alliances = PaginatedResource[AlliancesPage.AllianceHref]
	type MarketTypes = PaginatedResource[MarketTypesPage.Item]
	type KillMails = PaginatedResource[IdCrestLink[KillMail]]
	type Wars = PaginatedResource[IdCrestLink[War]]
}
