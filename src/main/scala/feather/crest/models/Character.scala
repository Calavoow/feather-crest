package feather.crest.models

import feather.crest.api.CrestLink
import feather.crest.api.CrestLink.CrestProtocol._

case class Decode(character: CrestLink[Character])

object Character {

	/**
	 * The bloodline of a character.
	 *
	 * @param href Unimplemented
	 */
	case class BloodLine(
		href: String,
		id: Int,
		id_str: String
	)

	/**
	 * The race of the character.
	 *
	 * @param href Unimplemented
	 */
	case class Race(
		href: String,
		id: Int,
		id_str: String
	)

}

case class Character(
	standings: TodoCrestLink,
	bloodLine: Character.BloodLine,
	gender_str: String,
	`private`: UnImplementedCrestLink,
	channels: UnImplementedCrestLink,
	// href: String, // Refers to this page? Left out because of 23-arity
	accounts: UnImplementedCrestLink,
	portrait: Picture,
	id: Int,
	blocked: TodoCrestLink,
	statistics: TodoCrestLink,
	contacts: TodoCrestLink,
	corporation: Corporation,
	id_str: String,
	mail: TodoCrestLink,
	capsuleer: UnImplementedCrestLink,
	vivox: UnImplementedCrestLink,
	description: String,
	notifications: TodoCrestLink,
	name: String,
	gender: Int,
	race: Character.Race,
	deposit: UnImplementedCrestLink
)


