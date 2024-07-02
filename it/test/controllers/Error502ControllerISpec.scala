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
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock._
import config.CustomJsonErrorHandler
import org.scalatest.Assertion
import play.api.Logger
import play.api.libs.json.{JsString, Json}

import java.util.UUID

class Error502ControllerISpec extends BaseISpec with NsiStubs with JsonGenerators {
  withClient { wsClient =>
    val endpoints = Table(
      ("Name", "TFC URL", "NSI Mapping", "Valid Payload"),
      ("link", "/link", nsiLinkAccountsEndpoint, randomLinkRequestJson),
      ("balance", "/balance", nsiCheckBalanceEndpoint, randomSharedJson),
      ("payment", "/", nsiMakePaymentEndpoint, randomPaymentRequestJson)
    )

    val tfc502scenarios = Table(
      ("NSI Status Code", "NSI Error Code", "Expected Error Code", "Expected Error Description"),
      (400, "E0006", "BAD_GATEWAY", "Bad Gateway"),
      (500, "E9000", "BAD_GATEWAY", "Bad Gateway"),
      (500, "E9999", "BAD_GATEWAY", "Bad Gateway"),
      (400, "E0022", "BAD_GATEWAY", "Bad Gateway")
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

        forAll(tfc502scenarios) {
          (nsiStatusCode, nsiErrorCode, expectedErrorCode, expectedErrorDescription) =>
            s"respond with status 502, errorCode $expectedErrorCode, and errorDescription \"$expectedErrorDescription\"" when {
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

                  response.status shouldBe BAD_GATEWAY
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
