package feather.crest

import feather.crest.api.CrestCollection

package object models {
	type ItemTypes = CrestCollection[NamedCrestLink[ItemType]]
	type ItemGroups = CrestCollection[NamedCrestLink[ItemGroup]]
	type Alliances = CrestCollection[AllianceLink]
	type MarketTypes = CrestCollection[MarketTypesPage]
	type KillMails = CrestCollection[IdCrestLink[KillMail]]
	type Wars = CrestCollection[IdCrestLink[War]]
}
