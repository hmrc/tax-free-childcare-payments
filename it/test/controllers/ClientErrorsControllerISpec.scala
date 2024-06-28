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
import org.scalatest.{Assertion, LoneElement}
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.util.UUID

class ClientErrorsControllerISpec
    extends BaseISpec
    with NsiStubs
    with LogCapturing
    with LoneElement
    with JsonGenerators {

  withClient { wsClient =>
    val endpoints = Table(
      ("Name", "TFC URL", "NSI Mapping", "Valid Payload"),
      ("link", "/link", nsiLinkAccountsEndpoint, randomLinkRequestJson),
      ("balance", "/balance", nsiCheckBalanceEndpoint, randomSharedJson),
      ("payment", "/", nsiMakePaymentEndpoint, randomPaymentRequestJson)
    )


    val tfc400scenarios = Table(
      ("NSI Status Code", "NSI Error Code", "Expected Error Code", "Expected Error Description"),
      (400, "E0008", "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0009", "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0010", "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0020", "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0021", "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0024", "BAD_REQUEST", "Request data is invalid or missing")
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


        forAll(tfc400scenarios) {
          (nsiStatusCode, nsiErrorCode, expectedErrorCode, expectedErrorDescription) =>
            s"respond with status 400, errorCode $expectedErrorCode, and errorDescription \"$expectedErrorDescription\"" when {
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

                  response.status shouldBe BAD_REQUEST
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

  private def withAuthNinoRetrievalExpectLog(
      expectedEndpoint: String,
      expectedCorrelationId: String
    )(
      doTest: => Assertion
    ): Unit = {
    withCaptureOfLoggingFrom(
      Logger(classOf[CustomJsonErrorHandler])
    ) { logs =>
      withAuthNinoRetrieval {
        doTest
      }

      val log = logs.loneElement
      log.getLevel shouldBe Level.INFO
      log.getMessage match {
        case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
          loggedEndpoint shouldBe expectedEndpoint
          loggedCorrelationId shouldBe expectedCorrelationId
          loggedMessage should startWith("Json validation error")

        case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
      }
    }
  }
}
