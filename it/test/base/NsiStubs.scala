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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.jdk.CollectionConverters.MapHasAsJava

/** The specs below should follow the NSI documentation at <https://drive.google.com/drive/folders/1ES36CjJpVumXXCM8VC5VQQa7J3xIIqoW>. */
trait NsiStubs { self: GuiceOneServerPerSuite =>

  /** NSI Link Accounts spec */

  protected def stubNsiLinkAccounts201(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiLinkAccountsEndpoint
      .withRequestBody(nsiLinkAccountsRequestBodyPattern)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected lazy val nsiLinkAccountsEndpoint: MappingBuilder = post(nsiLinkAccountsUrlPattern)

  private lazy val nsiLinkAccountsUrlPattern                 = nsiUrlPattern("linkAccounts", "[a-zA-Z0-9]+")
  private lazy val nsiLinkAccountsRequestBodyPattern         = jsonPatternFrom("eppURN,eppAccount,parentNino,childDoB")

  /** NSI Check Balance spec */

  protected def stubNsiCheckBalance200(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiCheckBalanceEndpoint
      .withQueryParams(nsiBalanceUrlQueryParams)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected lazy val nsiCheckBalanceEndpoint: MappingBuilder = get(nsiBalanceUrlPattern)

  private lazy val nsiBalanceUrlQueryParams = Map(
    "eppURN"     -> matching("[a-zA-Z0-9]+"),
    "eppAccount" -> matching("[a-zA-Z0-9]+"),
    "parentNino" -> matching(raw"[A-Z]{2}\d{6}[A-D]")
  ).asJava

  private lazy val nsiBalanceUrlPattern = nsiUrlPattern("checkBalance", raw"[a-zA-Z0-9]+\\?[^/]+")

  /** NSI Make Payment spec */

  protected def stubNsiMakePayment201(expectedResponseJson: JsValue): StubMapping = stubFor {
    nsiMakePaymentEndpoint
      .withRequestBody(nsiPaymentRequestBodyPattern)
      .willReturn(created() withBody expectedResponseJson.toString)
  }

  protected lazy val nsiMakePaymentEndpoint: MappingBuilder = post(nsiPaymentUrlPattern)

  private lazy val nsiPaymentUrlPattern         = nsiUrlPattern("makePayment")
  private lazy val nsiPaymentRequestBodyPattern = jsonPatternFrom("payeeType,amount,childAccountPaymentRef,eppURN,eppAccount,parentNino")

  /** Utils */

  private def nsiUrlPattern(endpointName: String, pathPatterns: String*) = {
    val initPath   = nsiRootPath + nsiConfig.get[String](endpointName)
    val urlPattern = pathPatterns.foldLeft(initPath)(_ + "/" + _)
    urlMatching(urlPattern)
  }

  private def jsonPatternFrom(expectedProps: String) =
    expectedProps
      .split(",")
      .map(prop => matchingJsonPath(s"$$.$prop"))
      .reduce(_ and _)

  private lazy val nsiConfig   = app.configuration.get[Configuration]("microservice.services.nsi")
  private lazy val nsiRootPath = nsiConfig.get[String]("rootPath")
}
