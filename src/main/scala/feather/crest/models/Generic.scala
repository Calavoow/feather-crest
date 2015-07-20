package feather.crest.models

import spray.json.JsonReader
import feather.crest.api.{AuthedAsyncIterable, CrestLink}

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
case class NamedCrestLink[T: JsonReader](override val href: String, name: String) extends CrestLink[T](href)

case class IdCrestLink[T: JsonReader](
	id_str: String,
	override val href: String,
	id: Int
) extends CrestLink[T](href)

case class IdNamedCrestLink[T: JsonReader](
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

/**
 * A resource that is listed across many pages.
 *
 * Do not use this directly in a model. Instead use / make a type alias in the package object.
 * @example [[ItemTypes]]
 * @todo Make next and previous the same type as 'this'. (Maybe type classes)
 * @tparam T The type of the Model being iterated over.
 */
case class PaginatedResource[T](
	totalCount_str: String,
	pageCount: Int,
	items: List[T],
	totalCount: Int,
	pageCount_str: String,
	next: Option[CrestLink[PaginatedResource[T]]],
	previous: Option[CrestLink[PaginatedResource[T]]]
) extends AuthedAsyncIterable[PaginatedResource[T]]
