package feather.crest.api

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import dispatch._
import feather.crest.api.CrestLink.CrestCommunicationException
import feather.crest.cache.{ExpiringCache, NoCache}
import feather.crest.models._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

object CrestLink {
	def apply[T : JsonReader](href : String) = new CrestLink[T](href)
	case class CrestCommunicationException(errorCode: Int, msg: String) extends RuntimeException(msg)

	/**
	 * Defines the JSON deserialisation protocols related to the Crest classes.
	 */
	object CrestProtocol extends DefaultJsonProtocol {
		/**
		 * Manually implemented json formatter for CrestLink, so that it can be a normal class for extension.
		 */
		implicit def crestLinkFormat[T : JsonFormat] : RootJsonFormat[CrestLink[T]] = new RootJsonFormat[CrestLink[T]] {
			override def write(c: CrestLink[T]) = JsObject(
				"href" -> JsString(c.href)
			)
			override def read(value: JsValue) = {
				value.asJsObject.getFields("href") match {
					case Seq(JsString(href)) =>
						new CrestLink[T](href)
					case _ => throw new DeserializationException("Crestlink expected")
				}
			}
		}

		implicit def namedCrestLinkFormat[T: JsonFormat]: JsonFormat[NamedCrestLink[T]] = jsonFormat(NamedCrestLink.apply[T] _, "href", "name")
		implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)
		implicit val unImplementedNamedFormat: JsonFormat[UnImplementedNamedCrestLink] = jsonFormat2(UnImplementedNamedCrestLink)
		implicit val todoFormat: JsonFormat[TodoCrestLink] = jsonFormat1(TodoCrestLink)
		implicit val todoNamedFormat: JsonFormat[TodoNamedCrestLink] = jsonFormat2(TodoNamedCrestLink)
		implicit val unCompletedFormat: JsonFormat[UncompletedCrestLink] = jsonFormat1(UncompletedCrestLink)
		implicit val linkFormat: JsonFormat[Link] = jsonFormat1(Link)
		implicit val pictureFormat: JsonFormat[Picture] = jsonFormat4(Picture)
		implicit val corporationFormat: JsonFormat[Corporation] = jsonFormat6(Corporation)

		implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
		implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
		implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
		implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat2(Root.Industry)
		implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)

		implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))

		implicit val regionFormat: JsonFormat[Region] = lazyFormat(jsonFormat5(Region.apply))

		implicit val itemTypesFormat: JsonFormat[ItemTypes] = lazyFormat(jsonFormat7(ItemTypes.apply))

		implicit val itemTypeFormat: JsonFormat[ItemType] = lazyFormat(jsonFormat2(ItemType.apply))

		implicit val marketOrdersFormat: JsonFormat[MarketOrders] = lazyFormat(jsonFormat7(MarketOrders.apply))
		implicit val marketOrdersReferenceFormat: JsonFormat[MarketOrders.Location] = jsonFormat4(MarketOrders.Location)
		implicit val MarketOrdersItemsFormat: JsonFormat[MarketOrders.Item] = jsonFormat17(MarketOrders.Item)

		implicit val marketHistoryFormat: JsonFormat[MarketHistory] = lazyFormat(jsonFormat5(MarketHistory.apply))
		implicit val marketHistoryItemsFormat: JsonFormat[MarketHistory.Item] = jsonFormat8(MarketHistory.Item)

		implicit val itemCategoriesFormat: JsonFormat[ItemCategories] = lazyFormat(jsonFormat5(ItemCategories.apply))

		implicit val itemCategoryFormat: JsonFormat[ItemCategory] = lazyFormat(jsonFormat3(ItemCategory.apply))

		implicit val itemGroupsFormat: JsonFormat[ItemGroups] = lazyFormat(jsonFormat7(ItemGroups.apply))

		implicit val itemGroupFormat: JsonFormat[ItemGroup] = lazyFormat(jsonFormat5(ItemGroup.apply))

		implicit val alliancesFormat: JsonFormat[Alliances] = lazyFormat(jsonFormat7(Alliances.apply))
		implicit val allianceLinkFormat: JsonFormat[Alliances.AllianceLink] = lazyFormat(jsonFormat5(Alliances.AllianceLink))

		implicit val allianceFormat: JsonFormat[Alliance] = jsonFormat14(Alliance.apply)
		implicit val allianceCharacterFormat: JsonFormat[Alliance.Character] = jsonFormat7(Alliance.Character)

		implicit val decodeFormat: JsonFormat[Decode] = lazyFormat(jsonFormat1(Decode))

		implicit val characterFormat: JsonFormat[Character] = lazyFormat(jsonFormat22(Character.apply))
		implicit val characterBloodlineFormat: JsonFormat[Character.BloodLine] = jsonFormat3(Character.BloodLine)
		implicit val characterRaceFormat: JsonFormat[Character.Race] = jsonFormat3(Character.Race)

		implicit val marketPricesFormat: JsonFormat[MarketPrices] = lazyFormat(jsonFormat5(MarketPrices.apply))
		implicit val marketPricesItemFormat: JsonFormat[MarketPrices.Item] = jsonFormat3(MarketPrices.Item)
	}
}
/**
 * CrestLink contains a crest URL to follow, creating another Crest instance
 * @param href The Crest URL to the next link
 * @tparam T The type of CrestContainer to construct.
 */
class CrestLink[T: JsonReader](val href: String) extends LazyLogging {
	/**
	 * Follow executes a request to the CREST API to instantiate the linked Crest class T.
	 *
	 * On failure halts webpage construction with HTTP error code.
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to retrieve cached results from and store the request result in.
	 * @return The constructed Crest class.
	 */
	def follow(auth: Option[String], params: Map[String, String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]): Future[T] = {
		val cacheResult = cache.get(cacheKey(params))
		cacheResult.getOrElse {
			request(auth, params)
		}
	}

	/**
	 * Ignore the cache and simply send a request.
	 *
	 * Note that the result of the request *is* stored in the cache,
	 * and thus the function does have a side-effect if the cache is not a stub.
	 * @param auth Authentication code of the EVE CREST endpoint.
	 * @param params GET Parameters for the request.
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to store the request result in.
	 * @return A model of T.
	 */
	def request(auth: Option[String], params: Map[String, String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]): Future[T] = {
		logger.trace(s"Fetching with {}", auth)
		// GET
		val getRequest = url(href).secure

		val acceptRequest = getRequest.setHeaders(Map(
			"Accept" -> Seq("application/json"),
			"Content-Type" -> Seq("application/vnd.ccp.eve.Api-v3+json; charset=utf-8"),
			"User-Agent" -> Seq("feather-crest/0.1")
		))
		// If the auth is set then add it as parameter.
		val authedRequest = auth.foldLeft(acceptRequest)((req, authKey) ⇒ {
			req.setHeader("Authorization", s"Bearer $authKey")
		})

		// Add the GET parameters
		val finalRequest = authedRequest <<? params

		logger.trace(s"Authedrequest string: ${authedRequest.toString}")
		logger.trace(finalRequest.toRequest.getUri.toString)

		val responseFut = Http(finalRequest)

		val parsedResult = responseFut.map { response ⇒
			response.getStatusCode match {
				case 200 =>
					logger.debug(s"Headers of request are: ${response.getHeaders()}")
					logger.debug(response.getStatusCode + ": " + response.getStatusText)
					val cacheTime = response.getHeader("Access-Control-Max-Age").toLong
					val cacheDuration = Duration(cacheTime, TimeUnit.SECONDS)
					val jsonAst = response.getResponseBody.parseJson
					val result = jsonAst.convertTo[T]
					val genValue = () => Future.successful(result)
					cache.apply(cacheKey(params), genValue, cacheDuration)
					result
				case 401 =>
					val message = s"Unauthorized authentication token. CREST response body: ${response.getResponseBody}"
					logger.warn(message)
					throw CrestCommunicationException(401, message)

			}
		}

		parsedResult.onFailure {
			case x ⇒ logger.warn(s"Error following link: $finalRequest\n ${x.getMessage}")
		}
		parsedResult
	}

	/**
	 * Constructs a fake url string, which uniquely identifies the real request.
	 * @param params The parameters found in the request.
	 * @return A unique fake url for the given `href` and `params`.
	 */
	private def cacheKey(params: Map[String, String]) : String = {
		// The key will look like query parameters (url?href?key=value&key2=value2)
		href + "?" + params.map {
			case (s1, s2) ⇒ s1 + "=" + s2
		}.mkString("&")
	}
}

/**
 * A CrestLink with already bound parameters.
 *
 * This class is used in Crestlink constructor functions in Models
 * that take some specific arguments and turn them into GET parameters.
 *
 * E.g. `Models.Region.marketBuyLink`.
 *
 * @param href The Crest URL to the next link
 * @param params The parameters to use in the follow method.
 * @tparam T The type of CrestContainer to construct.
 */
class CrestLinkParams[T: JsonReader](href: String, params: Map[String,String]) extends CrestLink[T](href) {
	/**
	 * Follow the crest link to construct a model of T.
	 *
	 * This function could not be overloaded without params, because of default arguments.
	 *
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to retrieve cached results from and store the request result in.
	 * @return The constructed Crest class.
	 */
	override def follow(auth: Option[String], params: Map[String,String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]) : Future[T] = {
		// No user defined parameters.
		if(params.isEmpty) super.follow(auth, this.params)
		else super.follow(auth, params)
	}

	/**
	 * Ignore the cache and simply send a request.
	 *
	 * Note that the result of the request *is* stored in the cache,
	 * and thus the function does have a side-effect if the cache is not a stub.
	 * @param auth Authentication code of the EVE CREST endpoint.
	 * @param params GET Parameters for the request.
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to store the request result in.
	 * @return A model of T.
	 */
	override def request(auth: Option[String], params: Map[String, String] = Map.empty)
	           (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]) : Future[T] = {
		// No user defined parameters.
		if(params.isEmpty) super.request(auth, this.params)
		else super.request(auth, params)
	}
}