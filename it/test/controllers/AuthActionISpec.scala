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

package controllers

import base.BaseSpec
import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, stubFor}
import org.scalatest.Assertion
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

class AuthActionISpec
    extends BaseSpec
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status
    with TableDrivenPropertyChecks {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort
    ).build()

  withClient { wsClient =>
    val contextRoot = "/individuals/tax-free-childcare/payments"
    val baseUrl     = s"http://localhost:$port$contextRoot"

    val resources = Table(
      "URL"               -> "Valid Payload",
      s"$baseUrl/link"    -> randomLinkRequestJson,
      s"$baseUrl/balance" -> randomMetadataJson,
      s"$baseUrl/"        -> randomPaymentRequestJson
    )

    /** Covers `case None` of [[controllers.actions.AuthAction.invokeBlock().]] */
    forAll(resources) { (url, payload) =>
      s"POST $url" should {
        s"respond $BAD_REQUEST" when {
          s"request header $CORRELATION_ID is missing" in withAuthNinoRetrieval {
            val res = wsClient
              .url(url)
              .withHttpHeaders(
                AUTHORIZATION -> "Bearer qwertyuiop"
              )
              .post(payload)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe Json.obj()
          }

          s"request header $CORRELATION_ID is not a valid UUID" in withAuthNinoRetrieval {
            val res = wsClient
              .url(url)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> "I am an invalid UUID."
              )
              .post(payload)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe Json.obj()
          }
        }
      }
    }
  }

  private def withAuthNinoRetrieval(check: => Assertion) = {
    stubFor(
      post("/auth/authorise") willReturn okJson(Json.obj("nino" -> "QW123456A").toString)
    )

    check
  }

  lazy val CORRELATION_ID = "Correlation-ID"
}
