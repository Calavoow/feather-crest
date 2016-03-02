package feather.crest.api

import com.typesafe.scalalogging.LazyLogging
import feather.crest.api.CrestLink.CrestProtocol._
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
			List(0,1) should contain(char.gender)
		}
	}

	it should "parse location data" in {
		val loc = for(
			r <- Root.fetch();
			decode <- r.decode.follow(auth);
			char <- decode.character.follow(auth);
			location <- char.location.follow(auth)
		) yield location

		whenReady(loc) { location =>
			location.solarSystem.foreach { sys =>
				sys.name should not be empty
			}
		}
	}

	ignore should "post waypoint" in {
		val waypoints = Waypoints(
			clearOtherWaypoints = true, first = true,
			IdCrestLink[SolarSystem](id = 30000001, id_str="30000001", href = "https://crest-tq.eveonline.com/solarsystems/30000001/")
		)

		val post = for(
			r <- Root.fetch();
			dec <- r.decode.follow(auth);
			character <- dec.character.follow(auth);
			posted <- character.waypoints.post(waypoints, auth=auth)
		) yield posted

		whenReady(post) { res =>
			logger.info(s"Waypoint return value:\n${res.getStatusCode}\n${res.getStatusText}\n${res.getResponseBody}")
			res.getStatusCode should equal(200)
		}
	}
}
