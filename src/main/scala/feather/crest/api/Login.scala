package feather.crest.api

import java.net.SocketTimeoutException

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.config.Config
import dispatch._, Defaults._

object Login extends LazyLogging {

	def loginUrl(csrfToken: String) = {
		val oathURL = "https://login.eveonline.com/oauth/authorize/"
		val redirectURL = "http://localhost:8080/login"
		val config = Config.readApiConfig
		s"$oathURL?" +
			s"response_type=code" +
			s"&client_id=${config.clientId}" +
			s"&scope=publicData" +
			s"&redirect_uri=$redirectURL" +
			s"&state=$csrfToken"
	}

	def exchangeAccessCode(accessCode: String) = exchangeCode(accessCode, "code", "authorization_code")

	def exchangeRefreshToken(token: String) = exchangeCode(token, "refresh_token", "refresh_token")

	def exchangeCode(accessCode: String, codeParam: String, grantType: String): Future[String] = {
		val tokenEndpoint = host("https://login.eveonline.com/").secure / "oauth" / "token"
		val config = Config.readApiConfig
		val request = tokenEndpoint.POST
			.<<(List("grant_type" → grantType, codeParam → accessCode))
			.<:<(List("clientid" → config.clientId, "key" → config.secretKey))
		logger.trace(s"AccessCode Request: ${request}")

		val result = Http(request OK as.String)
		result.failed.foreach { thr ⇒
			logger.info(s"Failure when exchanging access code: $thr")
		}
		result
	}
}
