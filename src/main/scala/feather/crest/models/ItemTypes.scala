package feather.crest.models

import com.typesafe.scalalogging.LazyLogging
import feather.crest.api.{CrestLink, AuthedAsyncIterable}
import feather.crest.api.CrestLink.CrestProtocol._

import scala.concurrent.ExecutionContext

case class ItemTypes(totalCount_str: String,
	pageCount: Int,
	items: List[NamedCrestLink[ItemType]],
	next: Option[CrestLink[ItemTypes]],
	totalCount: Int,
	pageCount_str: String,
	previous: Option[CrestLink[ItemTypes]]
)
	extends AuthedAsyncIterable[ItemTypes] with LazyLogging {
	def authedIterator(auth: Option[String], retries: Int = 1)
			(implicit ec: ExecutionContext) = {
		if( next.isDefined ) logger.info(s"Itemtypes has next: $next")
		// Cannot partially apply function, because of the implicit execution context.
		this.paramsIterator(Map.empty)(auth, retries)
	}
}

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

case class ItemGroups(
	totalCount_str: String,
	pageCount: Int,
	items: List[NamedCrestLink[ItemGroup]],
	next: Option[CrestLink[ItemGroups]],
	previous: Option[CrestLink[ItemGroups]],
	totalCount: Int,
	pageCount_str: String
) extends AuthedAsyncIterable[ItemGroups]

case class ItemGroup(
	category: CrestLink[ItemCategory],
	description: String,
	name: String,
	types: List[NamedCrestLink[ItemType]],
	published: Boolean
)

