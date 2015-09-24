package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import feather.crest.api.TwitterConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class ItemTypeSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

	"itemTypes" should "fetch an itemtype" in {
		val itemTypes = for(
			r <- Root.fetch();
			iTypes <- r.itemTypes.follow(auth)
		) yield iTypes

		whenReady(itemTypes) { iTypes =>
			// Iterator over all itemtypes pages
			val iterator = iTypes.paramsIterator(auth)
			val allItemTypes = twitterToScalaFuture(iterator.map(_.items).reduceLeft(_ ++ _))

			whenReady(allItemTypes) { allIT =>
				allIT.size should equal(iTypes.totalCount)

				val head = allIT.head
				// Check if Itemtype is deserialised correctly.
				val headItemType = head.follow(auth)
				whenReady(headItemType) { itemType =>
					// Check if name still corresponds.
					itemType.name should equal(head.name)
				}
			}
		}
	}
}
