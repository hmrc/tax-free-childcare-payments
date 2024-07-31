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

package base

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.{StringValuePattern, UrlPattern}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava

/** The specs below should follow the NSI documentation at <https://drive.google.com/drive/folders/1ES36CjJpVumXXCM8VC5VQQa7J3xIIqoW>. */
trait NsiStubs { self: GuiceOneServerPerSuite =>

  /** NSI Link Accounts spec */

  protected def stubNsiLinkAccounts201(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiLinkAccountsEndpoint
      .withQueryParams(nsiLinkAccountsUrlQueryParams)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected def stubNsiLinkAccountsError(status: Int, errorCode: String, errorDesc: String): StubMapping = stubFor {
    nsiLinkAccountsEndpoint willReturn nsiErrorResponse(status, errorCode, errorDesc)
  }

  protected lazy val nsiLinkAccountsEndpoint: MappingBuilder = get(nsiLinkAccountsUrlPattern)

  private lazy val nsiLinkAccountsUrlQueryParams = Map(
    "eppURN"     -> matching("[a-zA-Z0-9]+"),
    "eppAccount" -> matching("[a-zA-Z0-9]+"),
    "parentNino" -> matching(raw"[A-Z]{2}\d{6}[A-D]"),
    "childDoB"   -> matching(raw"\d{4}-\d{2}-\d{2}")
  ).asJava

  protected lazy val nsiLinkAccountsUrlPattern: UrlPattern = nsiUrlPattern("linkAccounts", raw"[a-zA-Z0-9]+\\?[^/]+")

  /** NSI Check Balance spec */

  protected def stubNsiCheckBalance200(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiCheckBalanceEndpoint
      .withQueryParams(nsiBalanceUrlQueryParams)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected def stubNsiCheckBalanceError(status: Int, errorCode: String, errorDesc: String): StubMapping = stubFor {
    nsiCheckBalanceEndpoint willReturn nsiErrorResponse(status, errorCode, errorDesc)
  }

  protected lazy val nsiCheckBalanceEndpoint: MappingBuilder = get(nsiBalanceUrlPattern)

  protected lazy val nsiBalanceUrlQueryParams: util.Map[String, StringValuePattern] = Map(
    "eppURN"     -> matching("[a-zA-Z0-9]+"),
    "eppAccount" -> matching("[a-zA-Z0-9]+"),
    "parentNino" -> matching(raw"[A-Z]{2}\d{6}[A-D]")
  ).asJava

  protected lazy val nsiBalanceUrlPattern: UrlPattern = nsiUrlPattern("checkBalance", raw"[a-zA-Z0-9]+\\?[^/]+")

  /** NSI Make Payment spec */

  protected def stubNsiMakePayment201(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiMakePaymentEndpoint
      .withRequestBody(nsiPaymentRequestBodyPattern)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected def stubNsiMakePaymentError(status: Int, errorCode: String, errorDesc: String): StubMapping = stubFor {
    nsiMakePaymentEndpoint willReturn nsiErrorResponse(status, errorCode, errorDesc)
  }

  protected lazy val nsiMakePaymentEndpoint: MappingBuilder = post(nsiPaymentUrlPattern)

  protected lazy val nsiPaymentUrlPattern: UrlPattern = nsiUrlPattern("makePayment")

  private lazy val nsiPaymentRequestBodyPattern = "payeeType,amount,childAccountPaymentRef,eppURN,eppAccount,parentNino"
    .split(",")
    .map(prop => matchingJsonPath(s"$$.$prop"))
    .reduce(_ and _)

  /** Utils */

  private def nsiErrorResponse(status: Int, errorCode: String, errorDesc: String) =
    aResponse()
      .withStatus(status)
      .withBody(Json.obj("errorCode" -> errorCode, "errorDescription" -> errorDesc).toString)

  private def nsiUrlPattern(endpointName: String, pathPatterns: String*) = {
    val initPath   = nsiRootPath + nsiConfig.get[String](endpointName)
    val urlPattern = pathPatterns.foldLeft(initPath)(_ + "/" + _)
    urlMatching(urlPattern)
  }

  private lazy val nsiConfig   = app.configuration.get[Configuration]("microservice.services.nsi")
  private lazy val nsiRootPath = nsiConfig.get[String]("rootPath")
}
