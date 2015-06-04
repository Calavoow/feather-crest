package feather.crest.api

import org.scalatest.{Matchers, FlatSpec}
import Models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ModelSpec extends FlatSpec with Matchers {
	"Root" should "be fetchable without auth" in {
		val root = Root.fetch(None)
		val rootRes = Await.result(root, 10 seconds)
		rootRes.crestEndpoint.href should be ("https://crest-tq.eveonline.com/")
	}
}
