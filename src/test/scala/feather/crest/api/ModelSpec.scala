package feather.crest.api

import java.io.{PrintWriter, File}

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, FlatSpec}
import Models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Success, Failure}
import TwitterConverters._

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
		root.onComplete {
			case Failure(ex) => fail("Failed to fetch root", ex)
			case Success(rootRes) =>
				rootRes.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
		}
		Await.ready(root, 10 seconds)
	}

	it should "test" in {
		import scala.concurrent.Await
		import scala.concurrent.duration._

		logger.info(s"Using authentication: $auth")

		val root = Root.fetch(None) // Future[Root] instance.
		// Follow the link to the itemtype page.
		val itemTypesPage = root.flatMap(_.itemTypes.follow(auth))
		/**
		 * The itemtypes are split over multiple pages (there are 30k+ of them), thus we create
		 * an asynchronous collection over all itemtype pages (a Twitter [[com.twitter.concurrent.Spool]])
		 * */
		val itemTypesSpool = itemTypesPage.map(_.authedIterator(auth, retries=3))
		// Flatten all itemTypes into one big list.
		val allItemTypes = itemTypesSpool.flatMap { itemType =>
			// Make one large List containing all itemTypes
			// (And implicitly convert Twitter Future -> Scala Future)
			itemType.map(_.items).reduceLeft(_ ++ _)
		}
		allItemTypes.foreach{ itemTypes =>
			// Lets print the first and last item type
			println(itemTypes.head)
			println(itemTypes.last)
		}

		Await.ready(allItemTypes, 30 seconds)
	}
}
