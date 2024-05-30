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

package config

import base.BaseISpec
import ch.qos.logback.classic.Level
import org.scalatest.{Assertion, LoneElement}
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.util.UUID

class CustomJsonErrorHandlerISpec extends BaseISpec with LogCapturing with LoneElement {

  withClient { wsClient =>
    val sharedBadRequestScenarios = Table(
      ("Spec", "Field", "Bad Value"),
      ("customer ID is invalid", "epp_unique_customer_id", "I am a bad customer ID."),
      ("registration ref is invalid", "epp_reg_reference", "I am a bad registration reference"),
      ("payment ref is invalid", "outbound_child_payment_ref", "I am a bad payment reference.")
    )

    val expectedCorrelationId = UUID.randomUUID().toString

    val linkEndpoint    = s"POST $resourcePath/link"
    val balanceEndpoint = s"POST $resourcePath/balance"
    val paymentEndpoint = s"POST $resourcePath/"

    linkEndpoint should {
      s"respond with $BAD_REQUEST and generic error message" when {
        forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
          spec in withAuthNinoRetrievalExpectLog("link", expectedCorrelationId) {
            val linkRequest = randomLinkRequestJson + (field, JsString(badValue))

            val res = wsClient
              .url(s"$baseUrl/link")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(linkRequest)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
          }
        }

        s"child DOB is invalid" in withAuthNinoRetrievalExpectLog("link", expectedCorrelationId) {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> "I am a bad date string"
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId
            )
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }
      }
    }

    balanceEndpoint should {
      s"respond with $BAD_REQUEST and generic error message" when {
        forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
          spec in withAuthNinoRetrievalExpectLog("balance", expectedCorrelationId) {
            val checkBalanceRequest = randomSharedJson + (field, JsString(badValue))

            val res = wsClient
              .url(s"$baseUrl/balance")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(checkBalanceRequest)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
          }
        }
      }
    }

    paymentEndpoint should {
      s"respond with $BAD_REQUEST and generic error message" when {
        forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
          spec in withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId) {
            val makePaymentRequest = randomPaymentRequestJson + (field, JsString(badValue))

            val res = wsClient
              .url(s"$baseUrl/")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(makePaymentRequest)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
          }
        }
      }
    }

    "GET /knil" should {
      s"respond with $NOT_FOUND and a JSON ErrorResponse" in {
        val res = wsClient
          .url(s"$baseUrl/knil")
          .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
          .get()
          .futureValue

        res.status shouldBe NOT_FOUND
        val resBody = res.json.as[ErrorResponse]
        resBody.statusCode shouldBe NOT_FOUND
      }
    }

    def withAuthNinoRetrievalExpectLog(
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
}
