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

import spray.json.JsonReader
import feather.crest.api.CrestLink

import scala.collection.mutable.ListBuffer
import scala.collection.{LinearSeqOptimized, LinearSeqLike, mutable, SeqLike}

/**
 * To follow this CrestLink some construction is required.
 *
 * See the class methods to construct a normal CrestLink
 */
case class UncompletedCrestLink(href: String)

/**
 * A CrestLink with a name field.
 *
 * @param href The Crest URL to the next link
 * @param name The name field.
 * @tparam T The type of CrestContainer to construct.
 */
case class NamedCrestLink[T: JsonReader](override val href: String, name: String) extends CrestLink[T](href)

/**
 * A [[feather.crest.api.CrestLink]] which has an [[id]] of the thing being linked.
 *
 * @param id_str The id stringified.
 * @param href The Crest URL to the Crest instance.
 * @param id The id of the item.
 * @tparam T The type of CrestContainer to construct.
 */
case class IdCrestLink[T: JsonReader](
	id_str: String,
	override val href: String,
	id: Int
) extends CrestLink[T](href)

/**
 * A [[CrestLink]] that also has an [[id]] and the [[name]] of the thing being linked.
 *
 * @param id_str id stringified
 * @param href The Crest URL to the item
 * @param id The id of the item
 * @param name The name of the item
 * @tparam T The type of CrestContainer to construct.
 */
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
 * Do not use this directly in a model. Instead make a type alias in the package object.
 *
 * @example [[ItemTypes]]
 * @todo Make next and previous the same type as 'this'. (Maybe type classes)
 * @tparam T The type of the Model being iterated over.
 */
case class Collection[T](
	totalCount_str: String,
	pageCount: Int,
	items: List[T],
	totalCount: Int,
	pageCount_str: String,
	next: Option[CrestLink[Collection[T]]],
	previous: Option[CrestLink[Collection[T]]]
)
