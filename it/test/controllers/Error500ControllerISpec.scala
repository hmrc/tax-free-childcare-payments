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

import base.{BaseISpec, JsonGenerators, NsiStubs}
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsString, Json}

import java.util.UUID

class Error500ControllerISpec extends BaseISpec with NsiStubs with JsonGenerators {
  withClient { wsClient =>
    val endpoints = Table(
      ("Name", "TFC URL", "NSI Mapping", "Valid Payload"),
      ("link", "/link", nsiLinkAccountsEndpoint, randomLinkRequestJson),
      ("balance", "/balance", nsiCheckBalanceEndpoint, randomSharedJson),
      ("payment", "/", nsiMakePaymentEndpoint, randomPaymentRequestJson)
    )

    val tfc500scenarios = Table(
      ("NSI Status Code", "NSI Error Code", "Expected Error Code", "Expected Error Description"),
      (400, "E0000", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0001", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0002", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0003", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0004", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0005", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0007", "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request")
    )

    val sharedBadRequestScenarios = Table(
      ("Spec", "Field", "Bad Value"),
      ("customer ID is invalid", "epp_unique_customer_id", "I am a bad customer ID."),
      ("registration ref is invalid", "epp_reg_reference", "I am a bad registration reference"),
      ("payment ref is invalid", "outbound_child_payment_ref", "I am a bad payment reference.")
    )

    forAll(endpoints) { (name, tfc_url, nsiMapping, validPayload) =>
      s"POST $tfc_url" should {
        s"respond with $BAD_REQUEST and generic error message" when {
          forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
            val expectedCorrelationId = UUID.randomUUID().toString

            spec in {
              val invalidPayload = validPayload + (field -> JsString(badValue))

              withAuthNinoRetrievalExpectLog(name, expectedCorrelationId) {
                val res = wsClient
                  .url(s"$baseUrl$tfc_url")
                  .withHttpHeaders(
                    AUTHORIZATION  -> "Bearer qwertyuiop",
                    CORRELATION_ID -> expectedCorrelationId
                  )
                  .post(invalidPayload)
                  .futureValue

                res.status shouldBe BAD_REQUEST
                res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
              }
            }
          }
        }

        forAll(tfc500scenarios) {
          (nsiStatusCode, nsiErrorCode, expectedErrorCode, expectedErrorDescription) =>
            s"respond with status 500, errorCode $expectedErrorCode, and errorDescription \"$expectedErrorDescription\"" when {
              s"NSI responds status code $nsiStatusCode and errorCode $nsiErrorCode" in {
                withAuthNinoRetrieval {
                  val nsiResponseBody = Json.obj("errorCode" -> nsiErrorCode)
                  val nsiResponse     = aResponse().withStatus(nsiStatusCode).withBody(nsiResponseBody.toString)
                  stubFor(nsiMapping willReturn nsiResponse)

                  val response = wsClient
                    .url(s"$baseUrl$tfc_url")
                    .withHttpHeaders(
                      AUTHORIZATION  -> "Bearer qwertyuiop",
                      CORRELATION_ID -> UUID.randomUUID().toString
                    )
                    .post(validPayload)
                    .futureValue

                  response.status shouldBe INTERNAL_SERVER_ERROR
                  response.json shouldBe Json.obj(
                    "errorCode"        -> expectedErrorCode,
                    "errorDescription" -> expectedErrorDescription
                  )
                }
              }
            }
        }
      }
    }
  }
}