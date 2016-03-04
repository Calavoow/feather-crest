/*
 * Feather-Crest is a library that simplifies interaction with the EVE Online API.
 * Copyright (C) 2016 Calavoow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package feather.crest.api

import java.util.concurrent.{Semaphore, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import dispatch._
import feather.crest.api.CrestLink.CrestCommunicationException
import feather.crest.cache.{ExpiringCache, NoCache}
import feather.crest.models._
import feather.crest.util.Util
import spray.json._

import scala.concurrent.{blocking, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object CrestLink {

	def apply[T : JsonReader](href : String) = new CrestLink[T](href)
	case class CrestCommunicationException(errorCode: Int, msg: String) extends RuntimeException(msg)

	// Rate limit set at 20 concurrent connections https://eveonline-third-party-documentation.readthedocs.org/en/latest/crest/intro/
	protected[api] val lock = new Semaphore(20,true)

	/**
	 * Defines the JSON deserialisation protocols related to the Crest classes.
	 */
	object CrestProtocol extends DefaultJsonProtocol {
		/**
		 * Generic
		 *
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


		/**
		 * Implementation of Root case class, since it has more than 22 fields and that is not supported by jsonFormat.
		 */
		implicit def rootFormat : RootJsonFormat[Root] = new RootJsonFormat[Root] {
			override def write(c: Root) = JsObject(
				"crestEndPoint" -> c.crestEndpoint.toJson,
				"corporationRoles" -> c.corporationRoles.toJson,
				"itemGroups" -> c.itemGroups.toJson,
				"channels" -> c.channels.toJson,
				"corporations" -> c.corporations.toJson,
				"alliances" -> c.alliances.toJson,
				"itemTypes" -> c.itemTypes.toJson,
				"decode" -> c.decode.toJson,
				"battleTheatres" -> c.battleTheatres.toJson,
				"marketPrices" -> c.marketPrices.toJson,
				"itemCategories" -> c.itemCategories.toJson,
				"regions" -> c.regions.toJson,
				"marketGroups" -> c.marketGroups.toJson,
				"systems" -> c.systems.toJson,
				"sovereignty" -> c.sovereignty.toJson,
				"tournaments" -> c.tournaments.toJson,
				"map" -> c.map.toJson,
				"wars" -> c.wars.toJson,
				"incursions" -> c.incursions.toJson,
				"authEndpoint" -> c.authEndpoint.toJson,
				"industry" -> c.industry.toJson,
				"clients" -> c.clients.toJson,
				"time" -> c.time.toJson,
				"marketTypes" -> c.marketTypes.toJson
			)
			override def read(value: JsValue) : Root = {
				val names = List(
					"crestEndpoint",
					"corporationRoles",
					"itemGroups",
					"channels",
					"corporations",
					"alliances",
					"itemTypes",
					"decode",
					"battleTheatres",
					"marketPrices",
					"itemCategories",
					"regions",
					"marketGroups",
					"systems",
					"sovereignty",
					"tournaments",
					"map",
					"wars",
					"incursions",
					"authEndpoint",
					"industry",
					"clients",
					"time",
					"marketTypes"
				)
				val fields = value.asJsObject.getFields(names:_*)

				// If the fields were correctly found.
				if(fields.size == names.size) {
					// Then deserialize JsObjects, by name
					// Cannot do unapply (case match), since more than 22 fields.
					val fieldMap = fields.zip(names).map { case (js, name) => name -> js }.toMap
					new Root(
						crestEndpoint = fieldMap("crestEndpoint").convertTo[CrestLink[Root]],
						corporationRoles = fieldMap("corporationRoles").convertTo[UnImplementedCrestLink],
						itemGroups = fieldMap("itemGroups").convertTo[ItemGroups],
						channels = fieldMap("channels").convertTo[UnImplementedCrestLink],
						corporations = fieldMap("corporations").convertTo[UnImplementedCrestLink],
						alliances = fieldMap("alliances").convertTo[Alliances],
						itemTypes = fieldMap("itemTypes").convertTo[ItemTypes],
						decode = fieldMap("decode").convertTo[CrestLink[Decode]],
						battleTheatres = fieldMap("battleTheatres").convertTo[UnImplementedCrestLink],
						marketPrices = fieldMap("marketPrices").convertTo[CrestLink[MarketPrices]],
						itemCategories = fieldMap("itemCategories").convertTo[CrestLink[ItemCategories]],
						regions = fieldMap("regions").convertTo[CrestLink[Regions]],
						marketGroups = fieldMap("marketGroups").convertTo[CrestLink[MarketGroups]],
						systems = fieldMap("systems").convertTo[CrestLink[Collection[IdNamedCrestLink[SolarSystem]]]],
						sovereignty = fieldMap("sovereignty").convertTo[Root.Sovereignty],
						tournaments = fieldMap("tournaments").convertTo[CrestLink[Tournaments]],
						map = fieldMap("map").convertTo[UnImplementedCrestLink],
						wars = fieldMap("wars").convertTo[Wars],
						incursions = fieldMap("incursions").convertTo[TodoCrestLink],
						authEndpoint = fieldMap("authEndpoint").convertTo[Link],
						industry = fieldMap("industry").convertTo[Root.Industry],
						clients = fieldMap("clients").convertTo[Root.Clients],
						time = fieldMap("time").convertTo[UnImplementedCrestLink],
						marketTypes = fieldMap("marketTypes").convertTo[MarketTypes]
					)
				} else {
					throw new DeserializationException("Root JSON expected.")
				}
			}
		}

		implicit def crestCollectionFormat[T : JsonFormat]: JsonFormat[CrestCollection[T]] = jsonFormat(CrestCollection.apply[T] _, "href")

		implicit def crestPostFormat[T: JsonFormat] : JsonFormat[CrestPost[T]] = jsonFormat(CrestPost.apply[T] _, "href")

		implicit def namedCrestLinkFormat[T: JsonFormat]: JsonFormat[NamedCrestLink[T]] = jsonFormat(NamedCrestLink.apply[T] _, "href", "name")
		implicit def idCrestLinkFormat[T: JsonFormat]: JsonFormat[IdCrestLink[T]] = jsonFormat(IdCrestLink.apply[T] _, "id_str", "href", "id")
		implicit def idNamedCrestLinkFormat[T: JsonFormat]: JsonFormat[IdNamedCrestLink[T]] = jsonFormat(IdNamedCrestLink.apply[T] _, "id_str", "href", "id", "name")
		implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)
		implicit val unImplementedNamedFormat: JsonFormat[UnImplementedNamedCrestLink] = jsonFormat2(UnImplementedNamedCrestLink)
		implicit val todoFormat: JsonFormat[TodoCrestLink] = jsonFormat1(TodoCrestLink)
		implicit val todoNamedFormat: JsonFormat[TodoNamedCrestLink] = jsonFormat2(TodoNamedCrestLink)
		implicit val unCompletedFormat: JsonFormat[UncompletedCrestLink] = jsonFormat1(UncompletedCrestLink)
		implicit val linkFormat: JsonFormat[Link] = jsonFormat1(Link)
		implicit val pictureFormat: JsonFormat[Picture] = jsonFormat4(Picture)
		implicit def paginatedResourceFormat[T: JsonFormat]: JsonFormat[Collection[T]] =
			lazyFormat(jsonFormat(Collection.apply[T] _, "totalCount_str", "pageCount", "items", "totalCount", "pageCount_str", "next", "previous"))

		/**
		 * Character
		 */
		implicit val decodeFormat: JsonFormat[Decode] = lazyFormat(jsonFormat1(Decode))

		implicit val characterFormat: JsonFormat[Character] = lazyFormat(jsonFormat22(Character.apply))
		implicit val characterBloodlineFormat: JsonFormat[Character.BloodLine] = jsonFormat3(Character.BloodLine)
		implicit val characterRaceFormat: JsonFormat[Character.Race] = jsonFormat3(Character.Race)

		implicit val locationFormat: JsonFormat[Location] = lazyFormat(jsonFormat1(Location))

		implicit val waypointsFormat: JsonFormat[Waypoints] = lazyFormat(jsonFormat3(Waypoints))

		/**
		 * Corporations
		 */
		implicit val corporationFormat: JsonFormat[Corporation] = jsonFormat6(Corporation)

		implicit val allianceLinkFormat: JsonFormat[AllianceLink] = lazyFormat(
			jsonFormat(AllianceLink.apply _, "id_str", "shortName", "href", "id", "name"))
//		implicit val allianceHrefFormat: JsonFormat[AlliancesPage.AllianceHref] = lazyFormat(jsonFormat1(AlliancesPage.AllianceHref))
		implicit val allianceFormat: JsonFormat[Alliance] = lazyFormat(jsonFormat14(Alliance.apply))
		implicit val allianceCharacterFormat: JsonFormat[Alliance.Character] =
			jsonFormat(Alliance.Character.apply _, "name", "isNPC", "href", "capsuleer", "portrait", "id", "id_str")

		// implicit val warsFormat: JsonFormat[Wars] = lazyFormat(jsonFormat7(Wars.apply))
		implicit val warFormat: JsonFormat[War] = lazyFormat(jsonFormat13(War.apply))
		implicit val warAllyFormat: JsonFormat[War.Ally] =
			jsonFormat(War.Ally.apply _, "name", "href", "id_str", "icon", "id")
		implicit val warBelligerentFormat: JsonFormat[War.Belligerent] =
			jsonFormat(War.Belligerent.apply _, "shipsKilled", "shipsKilled_str", "name", "href", "id_str", "icon", "id", "iskKilled")

		implicit val structuresFormat: JsonFormat[Structures] = lazyFormat(jsonFormat5(Structures.apply))
		implicit val structuresItemFormat: JsonFormat[Structures.Item] = lazyFormat(jsonFormat8(Structures.Item))

		implicit val campaignsFormat: JsonFormat[Campaigns] = lazyFormat(jsonFormat5(Campaigns.apply))
		implicit val campaignsItemFormat: JsonFormat[Campaigns.Item] = lazyFormat(jsonFormat12(Campaigns.Item))
		implicit val campaignsAttackersFormat: JsonFormat[Campaigns.Attackers] = jsonFormat1(Campaigns.Attackers)
		implicit val campaignsDefenderFormat: JsonFormat[Campaigns.Defender] = lazyFormat(jsonFormat2(Campaigns.Defender))
		implicit val campaignsScoreFormat: JsonFormat[Campaigns.Score] = lazyFormat(jsonFormat2(Campaigns.Score))

		/**
		 * Industry
		 */
		implicit val industryFacilitiesFormat: JsonFormat[IndustryFacilities] = lazyFormat(jsonFormat5(IndustryFacilities.apply))
		implicit val industryFacilitiesIdFormat: JsonFormat[IndustryFacilities.ID] = jsonFormat2(IndustryFacilities.ID)
		implicit val industryFacilitiesItemFormat: JsonFormat[IndustryFacilities.Item] = jsonFormat8(IndustryFacilities.Item)

		implicit val industrySystemsFormat: JsonFormat[IndustrySystems] = lazyFormat(jsonFormat5(IndustrySystems.apply))
		implicit val industrySystemCostIndexFormat: JsonFormat[IndustrySystems.SystemCostIndex] = jsonFormat4(IndustrySystems.SystemCostIndex)
		implicit val industrySystemsItemFormat: JsonFormat[IndustrySystems.Item] = lazyFormat(jsonFormat2(IndustrySystems.Item))
		/**
		 * ItemTypes
		 */
		implicit val itemTypeFormat: JsonFormat[ItemType] = lazyFormat(jsonFormat2(ItemType.apply))
		implicit val itemCategoriesFormat: JsonFormat[ItemCategories] = lazyFormat(jsonFormat5(ItemCategories.apply))
		implicit val itemCategoryFormat: JsonFormat[ItemCategory] = lazyFormat(jsonFormat3(ItemCategory.apply))
		implicit val itemGroupFormat: JsonFormat[ItemGroup] = lazyFormat(jsonFormat5(ItemGroup.apply))

		/**
		 * Market
		 */
		implicit val marketOrdersFormat: JsonFormat[MarketOrders] = lazyFormat(jsonFormat5(MarketOrders.apply))
		implicit val marketOrdersReferenceFormat: JsonFormat[MarketOrders.Location] = jsonFormat4(MarketOrders.Location)
		implicit val MarketOrdersItemFormat: JsonFormat[MarketOrders.Item] = jsonFormat17(MarketOrders.Item.apply)

		implicit val marketHistoryFormat: JsonFormat[MarketHistory] = lazyFormat(jsonFormat5(MarketHistory.apply))
		implicit val marketHistoryItemFormat: JsonFormat[MarketHistory.Item] = jsonFormat8(MarketHistory.Item)

		implicit val marketTypesPageItemFormat: JsonFormat[MarketTypesPage] = lazyFormat(jsonFormat2(MarketTypesPage.apply))
		implicit val marketTypesPageTypeFormat: JsonFormat[MarketTypesPage.Type] =
			jsonFormat(MarketTypesPage.Type.apply, "id_str", "href", "id", "name", "icon")

		implicit val marketGroupsFormat: JsonFormat[MarketGroups] = lazyFormat(jsonFormat5(MarketGroups.apply))
		implicit val marketGroupFormat: JsonFormat[MarketGroup] = lazyFormat(
			jsonFormat(MarketGroup.apply, "parentGroup", "href", "name", "types", "description"))

		implicit val marketPricesFormat: JsonFormat[MarketPrices] = lazyFormat(jsonFormat5(MarketPrices.apply))
		implicit val marketPricesItemFormat: JsonFormat[MarketPrices.Item] = jsonFormat3(MarketPrices.Item)

		/**
		 * Other
		 */
		implicit val tournamentsFormat: JsonFormat[Tournaments] = lazyFormat(jsonFormat5(Tournaments.apply))
		implicit val tournamentFormat: JsonFormat[Tournament] = lazyFormat(jsonFormat4(Tournament.apply))
		implicit val tournamentEntryFormat: JsonFormat[Tournament.Entry] = jsonFormat3(Tournament.Entry)

		implicit val killMailFormat: JsonFormat[KillMail] = lazyFormat(jsonFormat9(KillMail.apply))
		implicit def killMailImageLinkFormat[T: JsonFormat]: JsonFormat[KillMail.ImageLink[T]] =
			jsonFormat(KillMail.ImageLink.apply[T] _, "id_str", "href", "id", "name", "icon")
		implicit val killMailAttackerFormat: JsonFormat[KillMail.Attacker] = jsonFormat8(KillMail.Attacker)
		implicit val killMailVictimFormat: JsonFormat[KillMail.Victim] = lazyFormat(jsonFormat6(KillMail.Victim))
		implicit val killMailItemFormat: JsonFormat[KillMail.Item] = jsonFormat9(KillMail.Item)

		/**
		 * Root
		 */
//		implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
		implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
		implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
		implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat2(Root.Industry)
		implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)
		implicit val rootSovereigntyFormat: JsonFormat[Root.Sovereignty] = jsonFormat2(Root.Sovereignty)


		/**
		 * Universe
		 */
		implicit val positionFormat: JsonFormat[Position] = jsonFormat3(Position)

		implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))
		implicit val regionFormat: JsonFormat[Region] = lazyFormat(jsonFormat5(Region.apply))

		implicit val solarSystemFormat: JsonFormat[SolarSystem] = lazyFormat(jsonFormat9(SolarSystem.apply))

		implicit val constellationFormat: JsonFormat[Constellation] = lazyFormat(jsonFormat4(Constellation.apply))

		implicit val planetFormat: JsonFormat[Planet] = lazyFormat(jsonFormat4(Planet.apply))
	}
}
/**
 * CrestLink contains a crest URL to follow, creating another Crest instance
 *
 * @param href The Crest URL to the Crest instance.
 * @tparam T The type of CrestContainer to construct.
 */
class CrestLink[T: JsonReader](val href: String) extends LazyLogging {
	/**
	 * Follow executes a request to the CREST API to instantiate the linked Crest class T.
	 *
	 * On failure halts webpage construction with HTTP error code.
	 *
	 * @param auth The authentication key or None if unused.
	 * @param retries The number of times to retry getting the item. Default = 1.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to retrieve cached results from and store the request result in.
	 * @return The constructed Crest class.
	 */
	def follow(auth: Option[String] = None, retries : Int = 1, params: Map[String, String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]): Future[T] = {
		val cacheResult = cache.get(cacheKey(params))
		cacheResult.getOrElse {
			request(auth, retries, params)
		}
	}

	/**
	 * Ignore the cache and simply send a request.
	 *
	 * Note that the result of the request *is* stored in the cache,
	 * and thus the function does have a side-effect if the cache is not a stub.
	 *
	 * @param auth Authentication code of the EVE CREST endpoint.
	 * @param retries The number of times to retry getting the item. Default = 1.
	 * @param params GET Parameters for the request.
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to store the request result in.
	 * @return A model of T.
	 */
	def request(auth: Option[String] = None, retries: Int = 1, params: Map[String, String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]): Future[T] = {
		logger.trace(s"Fetching $href with $auth")
		// GET
		val getRequest = url(href).secure

		val acceptRequest = getRequest.setHeaders(Map(
			"Accept" -> Seq("application/json"),
			"Content-Type" -> Seq("application/vnd.ccp.eve.Api-v3+json; charset=UTF-8"),
			"User-Agent" -> Seq("feather-crest/0.1")
		))
		// If the auth is set then add it as parameter.
		val authedRequest = auth.foldLeft(acceptRequest)((req, authKey) ⇒ {
			req.setHeader("Authorization", s"Bearer $authKey")
		})

		// Add the GET parameters
		val finalRequest = authedRequest <<? params

		blocking(CrestLink.lock.acquire())
		val responseFut = Util.retryFuture(retries) {
			Http(finalRequest)
		}
		responseFut.onComplete { _ =>
			CrestLink.lock.release()
		}

		val parsedResult = responseFut.map { response ⇒
			response.getStatusCode match {
				case 200 =>
					logger.debug(s"Response status ${response.getStatusCode}: ${response.getStatusText} for\n$href with params ${params.mkString("Map(",",",")")}")
					val cacheTime = response.getHeader("Access-Control-Max-Age").toLong
					val cacheDuration = Duration(cacheTime, TimeUnit.SECONDS)
					val jsonAst = response.getResponseBody.parseJson
					try {
						val result = jsonAst.convertTo[T]
						val genValue = () => Future.successful(result)
						cache.apply(cacheKey(params), genValue, cacheDuration)
						result
					} catch {
						case ex@DeserializationException(msg, _, _) =>
							logger.error(s"Error deserializing $href")
							throw ex
					}
				case code =>
					logger.warn(s"Failure code: $code, message:\n${response.getResponseBody}")
					throw CrestCommunicationException(code, s"${response.getResponseBody}")
			}
		}

		parsedResult.onFailure {
			case x ⇒
				logger.warn(s"Error following link: $this\n${x.getMessage}\n${x.getStackTrace.mkString("", "\n", "\n")}")
		}
		parsedResult
	}

	/**
	 * Constructs a fake url string, which uniquely identifies the real request.
	 *
	 * @param params The parameters found in the request.
	 * @return A unique fake url for the given `href` and `params`.
	 */
	private def cacheKey(params: Map[String, String]) : String = {
		// The key will look like query parameters (url?href?key=value&key2=value2)
		href + "?" + params.map {
			case (s1, s2) ⇒ s1 + "=" + s2
		}.mkString("&")
	}

	override def toString: String = "CrestLink(" + href + ")"
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
	 * Because of already bound parameters, provide params at your own risk.
	 *
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to retrieve cached results from and store the request result in.
	 * @return The constructed Crest class.
	 */
	override def follow(auth: Option[String] = None, retries: Int = 1, params: Map[String,String] = Map.empty)
	          (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]) : Future[T] = {
		// No user defined parameters.
		if(params.isEmpty) super.follow(auth, retries, this.params)
		else super.follow(auth, retries, params)
	}

	/**
	 * Ignore the cache and simply send a request.
	 *
	 * Note that the result of the request *is* stored in the cache,
	 * and thus the function does have a side-effect if the cache is not a stub.
	 *
	 * @param auth Authentication code of the EVE CREST endpoint.
	 * @param params GET Parameters for the request.
	 * @param ec The execution context for the Futures.
	 * @param cache The cache to store the request result in.
	 * @return A model of T.
	 */
	override def request(auth: Option[String] = None, retries: Int = 1, params: Map[String, String] = Map.empty)
	           (implicit ec: ExecutionContext, cache: ExpiringCache[T] = new NoCache[T]) : Future[T] = {
		// No user defined parameters.
		if(params.isEmpty) super.request(auth, retries, this.params)
		else super.request(auth, retries, params)
	}
}