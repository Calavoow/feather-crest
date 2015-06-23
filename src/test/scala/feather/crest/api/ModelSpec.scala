package feather.crest.api

import java.io.{PrintWriter, File}

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, FlatSpec}
import Models._
import scala.collection.TraversableLike
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

class ModelSpec extends FlatSpec with Matchers with LazyLogging {
	/**
	 * Try to read auth.txt in the test/resources folder.
	 *
	 * This file should only contain a string of an authentication token,
	 * such that the authed-crest may be accessed with it.
	 */
	val auth = {
		Option(getClass().getResource("/auth.txt"))
			.map(Source.fromURL)
			.map(_.getLines().next())
	}

	"Root" should "be fetchable without auth" in {
		val root = Root.fetch(None)
		val rootRes = Await.result(root, 10 seconds)
		rootRes.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
	}

	it should "test" in {
		import scala.concurrent.Await
		import scala.concurrent.duration._

		println(auth)

		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		val itemTypes = root.flatMap(_.itemTypes.follow(auth))
		// Create a collection over all item type pages
		val allItemTypes = itemTypes.map(_.authedIterator(auth, retries=3))

		val resItemTypes = Await.result(allItemTypes, 3 seconds)
		val mappedItemtypes = resItemTypes.map({ itemType =>
			itemType.onSuccess {
				case x => logger.trace(s"User succes: $x")
			}
			itemType.map(_.items)
		})(AsyncIterator.canBuildFrom[List[NamedCrestLink[ItemType]]])
		mappedItemtypes.foreach { mType =>
			mType.onSuccess {
				case x => logger.trace(s"Map success: $x")
			}
		}

		this.synchronized {
			wait(60000)
		}

		// Block for at most 10 seconds to get the Root.
//		val rootResult = Await.result(root, 30 seconds) // Root
//		// Then print the href to the endpoint
//		println(rootResult.crestEndpoint.href)
//		root.foreach(r => println(r.regions.href))
//		val region = for(rootRes <- root;
//		    region <- rootRes.regions.follow(auth)) yield {
//			println(region.items.map(_.name).mkString(","))
//			region
//		}
//		Await.ready(region, 30 seconds)
	}
}
