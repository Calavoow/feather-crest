package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}
import feather.crest.models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import feather.crest.api.TwitterConverters._
import feather.crest.api.CrestLink.CrestProtocol._

class ModelSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	"Root" should "be fetchable without auth" in {
		implicit val patienceConfig = PatienceConfig(timeout = 3 seconds)
		val root = Root.fetch(None)
		whenReady(root) { readyRoot =>
			println(readyRoot)
			readyRoot.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
		}
	}
}
