package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class CharacterSpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {
	import Authentication.auth

	implicit override val patienceConfig = PatienceConfig(timeout = 10.seconds)

	"character" should "fetch user Decode and Character" in {
		val root = Root.fetch()
		val character = for(
			r <- root;
			decode <- r.decode.follow(auth);
			char <- decode.character.follow(auth)
		) yield char

		whenReady(character) { char =>
			char.id.toString should equal(char.id_str)
		}
	}
}
