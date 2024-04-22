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

import models.requests.LinkResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

class TaxFreeChildcarePaymentsControllerISpec
    extends AnyWordSpec
    with should.Matchers
    with WireMockSupport
    with ScalaFutures
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status {

  import com.github.tomakehurst.wiremock.client.WireMock._
  import play.api.Application
  import play.api.libs.json.Json

  override lazy val wireMockPort = 10501

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wireMockPort)
    .build()

  withClient { wsClient =>
    val baseUrl = s"http://localhost:$port"

    /** Covers [[TaxFreeChildcarePaymentsController.link]] */
    "POST /link" should {
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val nsiResponse = okJson(Json.toJson(LinkResponse("", "")).toString)
          stubFor(
            post("/individuals/tax-free-childcare/payments/link") willReturn nsiResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> "",
            "epp_unique_customer_id"     -> "",
            "epp_reg_reference"          -> "",
            "outbound_child_payment_ref" -> "",
            "child_date_of_birth"        -> ""
          )

          val res = wsClient
            .url(s"$baseUrl/individuals/tax-free-childcare/payments/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe OK
        }
      }
    }
  }
}
