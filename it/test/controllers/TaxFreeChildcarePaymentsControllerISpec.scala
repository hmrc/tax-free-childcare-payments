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

import base.BaseISpec
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json

import java.util.UUID

class TaxFreeChildcarePaymentsControllerISpec extends BaseISpec {

  withClient { wsClient =>
    /** Covers [[TaxFreeChildcarePaymentsController.link()]] */
    "POST /link" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedResponseJson  = Json.obj(
            "child_full_name" -> "Peter Pan"
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/link")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponseJson.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomLinkRequestJson)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedResponseJson
        }
      }
    }

    /** Covers [[TaxFreeChildcarePaymentsController.balance()]] */
    "POST /balance" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedResponse      = Json.obj(
            "tfc_account_status" -> "active",
            "paid_in_by_you"     -> 0,
            "government_top_up"  -> 0,
            "total_balance"      -> 0,
            "cleared_funds"      -> 0,
            "top_up_allowance"   -> 0
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/balance")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponse.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomSharedJson)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedResponse
        }
      }
    }

    /** Covers [[TaxFreeChildcarePaymentsController.payment()]] */
    "POST /" should {
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedResponse = Json.obj(
            "payment_reference"      -> expectedPaymentRef,
            "estimated_payment_date" -> expectedPaymentDate
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponse.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomPaymentRequestJson)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedResponse
        }
      }
    }

    val endpoints = Table(
      ("Name", "TFC URL", "NSI URL", "Valid Payload"),
      ("link", s"$resourcePath/link", "/individuals/tax-free-childcare/payments/link", randomLinkRequestJson),
      ("balance", s"$resourcePath/balance", "/individuals/tax-free-childcare/payments/balance", randomSharedJson),
      ("payment", s"$resourcePath/", "/individuals/tax-free-childcare/payments/", randomPaymentRequestJson)
    )

    val nsiErrorScenarios = Table(
      ("NSI Error Code", "Expected Upstream Status Code", "Expected Error Code", "Expected Error Description"),
      ("E0000", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0001", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0002", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0003", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0004", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0005", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0006", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0007", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      ("E0008", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E0009", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E0010", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E0020", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E0021", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E0022", 502, "BAD_GATEWAY", "Bad Gateway"),
      ("E0024", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      ("E9000", 502, "BAD_GATEWAY", "Bad Gateway"),
      ("E9999", 502, "BAD_GATEWAY", "Bad Gateway"),
      ("E8000", 503, "SERVICE_UNAVAILABLE", "The service is currently unavailable"),
      ("E8001", 503, "SERVICE_UNAVAILABLE", "The service is currently unavailable")
    )

    forAll(endpoints) { (_, tfc_url, nsi_url, validPayload) =>
      s"POST $tfc_url" should forAll(nsiErrorScenarios) {
        (nsiErrorCode, expectedUpstreamStatusCode, expectedErrorCode, expectedErrorDescription) =>
          s"respond with status $expectedUpstreamStatusCode, errorCode $expectedErrorCode, and errorDescription \"$expectedErrorDescription\"" when {
            s"NSI responds with $nsiErrorCode" in withAuthNinoRetrieval {
              val nsiResponseBody = Json.obj("errorCode" -> nsiErrorCode)
              stubFor(
                post(nsi_url) willReturn aResponse().withBody(nsiResponseBody.toString)
              )

              val response = wsClient
                .url(s"$domain$tfc_url")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> UUID.randomUUID().toString
                )
                .post(validPayload)
                .futureValue

              val actualErrorCode        = (response.json \ "errorCode").as[String]
              val actualErrorDescription = (response.json \ "errorDescription").as[String]

              response.status shouldBe expectedUpstreamStatusCode
              actualErrorCode shouldBe expectedErrorCode
              actualErrorDescription shouldBe expectedErrorDescription
            }
          }
      }
    }
  }
}
