/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.matching.{StringValuePattern, UrlPattern}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalacheck.Gen
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.twirl.api.Html

import java.util
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.MapHasAsJava

trait NsiStubs extends Status { self: BaseISpec =>

  private val nsiConfig   = app.configuration.get[Configuration]("microservice.services.nsi")
  private val nsiRootPath = nsiConfig.get[String]("rootPath")

  /** NSI Link Accounts spec */

  protected def stubNsiLinkAccounts(
      status: Int,
      body: String,
      responseTime: Duration = Duration.Zero
  ): StubMapping = stubFor {
    nsiLinkAccountsEndpoint
      .withQueryParams(nsiLinkAccountsUrlQueryParams)
      .willReturn(response(status, body, responseTime))
  }

  protected def stubNsiLinkAccountsHtml(status: Int, htmlBody:String): StubMapping = stubFor {
    nsiLinkAccountsEndpoint
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Html(htmlBody).toString)
      )
  }

  protected def stubNsiLinkAccountsError(): StubMapping = stubFor {
    nsiLinkAccountsEndpoint
      .willReturn(
        serverError()
      )
  }

  protected val nsiLinkAccountsUrlPattern: UrlPattern = nsiUrlPattern("linkAccounts", raw"[a-zA-Z0-9]+\\?[^/]+")

  protected val nsiLinkAccountsEndpoint: MappingBuilder = get(nsiLinkAccountsUrlPattern)

  protected val nsiLinkAccountsUrlQueryParams: util.Map[String, StringValuePattern] = Map(
    "eppURN"     -> matching("[a-zA-Z0-9]+"),
    "eppAccount" -> matching("[a-zA-Z0-9]+"),
    "parentNino" -> matching(raw"[A-Z]{2}\d{6}[A-D]"),
    "childDoB"   -> matching(raw"\d{4}-\d{2}-\d{2}")
  ).asJava

  /** NSI Check Balance spec */

  protected def stubNsiBalanceCheck(
      status: Int,
      body: String,
      responseTime: Duration = Duration.Zero
  ): StubMapping = stubFor {
    nsiCheckBalanceEndpoint
      .withQueryParams(nsiBalanceUrlQueryParams)
      .willReturn(response(status, body, responseTime))
  }


  protected def stubNsiCheckBalanceHtml(status: Int, htmlBody:String): StubMapping = stubFor {
    nsiCheckBalanceEndpoint
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(htmlBody)
      )
  }

  protected def stubNsiCheckBalanceError(): StubMapping = stubFor {
    nsiCheckBalanceEndpoint
      .willReturn(
        serverError()
      )
  }

  protected val nsiBalanceUrlPattern: UrlPattern = nsiUrlPattern("checkBalance", raw"[a-zA-Z0-9]+\\?[^/]+")

  protected val nsiCheckBalanceEndpoint: MappingBuilder = get(nsiBalanceUrlPattern)

  protected val nsiBalanceUrlQueryParams: util.Map[String, StringValuePattern] = Map(
    "eppURN"     -> matching("[a-zA-Z0-9]+"),
    "eppAccount" -> matching("[a-zA-Z0-9]+"),
    "parentNino" -> matching(raw"[A-Z]{2}\d{6}[A-D]")
  ).asJava

  /** NSI Make Payment spec */

  protected def stubNsiMakePayment(
      status: Int,
      body: String,
      responseTime: Duration = Duration.Zero
  ): StubMapping = stubFor {
    nsiMakePaymentEndpoint
      .withRequestBody(nsiPaymentRequestBodyPattern)
      .willReturn(response(status, body, responseTime))
  }


  protected def stubNsiMakePaymentHtml(status: Int, htmlBody:String): StubMapping = stubFor {
    nsiMakePaymentEndpoint
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Html(htmlBody).toString)
      )
  }

  protected def stubNsiMakePaymentError(): StubMapping = stubFor {
    nsiMakePaymentEndpoint
      .willReturn(
        serverError()
      )
  }

  protected val nsiPaymentUrlPattern: UrlPattern = nsiUrlPattern("makePayment")

  protected val nsiMakePaymentEndpoint: MappingBuilder = post(nsiPaymentUrlPattern)

  private val nsiPaymentRequestBodyPattern = "payeeType,amount,childAccountPaymentRef,eppURN,eppAccount,parentNino"
    .split(",")
    .map(prop => matchingJsonPath(s"$$.$prop"))
    .reduce(_ and _)

  /** Utils */

  private def response(
      status: Int,
      body: String,
      responseTime: Duration = Duration.Zero
  ): ResponseDefinitionBuilder =
    aResponse().withStatus(status).withFixedDelay(responseTime.toMillis.toInt).withBody(body)

  private def nsiUrlPattern(endpointName: String, pathPatterns: String*) = {
    val initPath   = nsiRootPath + nsiConfig.get[String](endpointName)
    val urlPattern = pathPatterns.foldLeft(initPath)(_ + "/" + _)
    urlMatching(urlPattern)
  }

  /** Random data */

  protected val randomHttpErrorCodes: Gen[Int] = Gen.chooseNum(BAD_REQUEST, NETWORK_AUTHENTICATION_REQUIRED)

}
