package feather.crest.api

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import dispatch._
import feather.crest.api.CrestLink.CrestCommunicationException
import feather.crest.api.Models._
import feather.crest.cache.{ExpiringCache, NoCache}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

object CrestLink {
	case class CrestCommunicationException(errorCode: Int, msg: String) extends RuntimeException(msg)

	/**
	 * Defines the JSON deserialisation protocols related to the Crest classes.
	 */
	object CrestProtocol extends DefaultJsonProtocol {
		implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)
		implicit val unCompletedFormat: JsonFormat[UncompletedCrestLink] = jsonFormat1(UncompletedCrestLink)
		implicit val unImplementedNamedFormat: JsonFormat[UnImplementedNamedCrestLink] = jsonFormat2(UnImplementedNamedCrestLink)

		implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
		implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
		implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
		implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat2(Root.Industry)
		implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)

		implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))
		implicit val regionsCrestLinkFormat: JsonFormat[CrestLink[Regions]] = jsonFormat(CrestLink[Regions] _, "href")
		implicit val regionsItemFormat: JsonFormat[NamedCrestLink[Region]] = jsonFormat(NamedCrestLink[Region] _, "href", "name")

		implicit val regionFormat: JsonFormat[Region] = lazyFormat(jsonFormat5(Region.apply))
		implicit val regionCrestLinkFormat: JsonFormat[CrestLink[Region]] = jsonFormat(CrestLink[Region] _, "href")

		implicit val itemTypesFormat: JsonFormat[ItemTypes] = lazyFormat(jsonFormat7(ItemTypes.apply))
		implicit val itemTypesCrestLinkFormat: JsonFormat[CrestLink[ItemTypes]] = jsonFormat(CrestLink[ItemTypes] _, "href")

		implicit val itemTypeFormat: JsonFormat[ItemType] = lazyFormat(jsonFormat1(ItemType.apply))
		implicit val itemTypeCrestLinkFormat: JsonFormat[NamedCrestLink[ItemType]] = jsonFormat(NamedCrestLink[ItemType] _, "href", "name")

		implicit val marketOrdersFormat: JsonFormat[MarketOrders] = lazyFormat(jsonFormat7(MarketOrders.apply))
		implicit val marketOrdersCrestLinkFormat: JsonFormat[CrestLink[MarketOrders]] = jsonFormat(CrestLink[MarketOrders] _, "href")
		implicit val marketOrdersLocationFormat: JsonFormat[MarketOrders.Reference] = jsonFormat4(MarketOrders.Reference)
		implicit val MarketOrdersItemsFormat: JsonFormat[MarketOrders.Item] = jsonFormat17(MarketOrders.Item)

		implicit val marketHistoryFormat: JsonFormat[MarketHistory] = lazyFormat(jsonFormat5(MarketHistory.apply))
		implicit val marketHistoryCrestLinkFormat: JsonFormat[CrestLink[MarketHistory]] = jsonFormat(CrestLink[MarketHistory] _, "href")
		implicit val marketHistoryItemsFormat: JsonFormat[MarketHistory.Item] = jsonFormat8(MarketHistory.Item)
	}
}

/**
 * CrestLink contains a crest URL to follow, creating another Crest instance
 * @param href The Crest URL to the next link
 * @tparam T The type of CrestContainer to construct.
 */
case class CrestLink[T: JsonReader](href: String) extends LazyLogging {
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