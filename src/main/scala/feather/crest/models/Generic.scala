package feather.crest.models

import spray.json.JsonFormat
import feather.crest.api.CrestLink
import feather.crest.api.CrestLink.CrestProtocol._

/**
 * To follow this crest link some construction is required.
 *
 * See the class methods to construct a normal CrestLink
 */
case class UncompletedCrestLink(href: String)

/**
 * A CrestLink with a name field.
 * @param href The Crest URL to the next link
 * @param name The name field.
 * @tparam T The type of CrestContainer to construct.
 */
case class NamedCrestLink[T: JsonFormat](override val href: String, name: String) extends CrestLink[T](href)

case class IdCrestLink[T: JsonFormat](
	id_str: String,
	override val href: String,
	id: Int
) extends CrestLink[T](href)

case class IdNamedCrestLink[T: JsonFormat](
	id_str: String,
	override val href: String,
	id: Int,
	name: String
) extends CrestLink[T](href)

/**
 * A CrestLink which has not been implemented by CCP.
 */
case class UnImplementedCrestLink(href: String)

/**
 * A CrestLink with a name that has not been implemented by CCP.
 */
case class UnImplementedNamedCrestLink(href: String, name: String)

/**
 * A CrestLink which has not been implemented by Feather-Crest.
 */
case class TodoCrestLink(href: String)

/**
 * A CrestLink which has not been implemented by Feather-Crest.
 */
case class TodoNamedCrestLink(href: String, name: String)

/**
 * A simple hyper-reference
 */
case class Link(href: String)

/**
 * A picture
 */
case class Picture(
	`32x32`: Link,
	`64x64`: Link,
	`128x128`: Link,
	`256x256`: Link
)
