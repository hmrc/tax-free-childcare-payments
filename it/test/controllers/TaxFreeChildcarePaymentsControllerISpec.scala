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
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock._
import config.CustomJsonErrorHandler
import org.scalatest.{Assertion, LoneElement}
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.util.UUID

class TaxFreeChildcarePaymentsControllerISpec extends BaseISpec with LogCapturing with LoneElement {

  withClient { wsClient =>
    "POST /link" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedResponseJson  = Json.obj(
            "child_full_name" -> "Peter Pan"
          )

          stubFor(
            post("/tax-free-childcare-payments-nsi-stub/link")
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

      s"respond with $BAD_REQUEST and generic error message" when {
        val expectedCorrelationId = UUID.randomUUID()

        s"child DOB is invalid" in withAuthNinoRetrievalExpectLog("link", expectedCorrelationId.toString) {
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
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          res.body shouldBe EXPECTED_JSON_ERROR_RESPONSE.toString
        }
      }
    }

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
            post("/tax-free-childcare-payments-nsi-stub/balance")
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
            post("/tax-free-childcare-payments-nsi-stub/")
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

      s"respond with $BAD_REQUEST and generic error message" when {
        val expectedCorrelationId = UUID.randomUUID()

        s"payment amount is invalid" in withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
          val invalidPaymentRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "payment_amount"             -> "I am a bad payment reference",
            "ccp_reg_reference"          -> "qwertyui",
            "ccp_postcode"               -> "AS12 3DF",
            "payee_type"                 -> randomPayeeType
          )

          val res = wsClient
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(invalidPaymentRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }
      }
    }

    val endpoints = Table(
      ("Name", "TFC URL", "NSI URL", "Valid Payload"),
      ("link", s"/link", "/tax-free-childcare-payments-nsi-stub/link", randomLinkRequestJson),
      ("balance", s"/balance", "/tax-free-childcare-payments-nsi-stub/balance", randomSharedJson),
      ("payment", s"/", "/tax-free-childcare-payments-nsi-stub/", randomPaymentRequestJson)
    )

    val nsiErrorScenarios = Table(
      ("NSI Status Code", "NSI Error Code", "Expected Upstream Status Code", "Expected Error Code", "Expected Error Description"),
      (400, "E0000", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0001", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0002", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0003", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0004", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0005", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0006", 502, "BAD_GATEWAY", "Bad Gateway"),
      (400, "E0007", 500, "INTERNAL_SERVER_ERROR", "The server encountered an error and couldn't process the request"),
      (400, "E0008", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0009", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0010", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0020", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0021", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (400, "E0022", 502, "BAD_GATEWAY", "Bad Gateway"),
      (400, "E0024", 400, "BAD_REQUEST", "Request data is invalid or missing"),
      (500, "E9000", 502, "BAD_GATEWAY", "Bad Gateway"),
      (500, "E9999", 502, "BAD_GATEWAY", "Bad Gateway"),
      (503, "E8000", 503, "SERVICE_UNAVAILABLE", "The service is currently unavailable"),
      (503, "E8001", 503, "SERVICE_UNAVAILABLE", "The service is currently unavailable")
    )

    val sharedBadRequestScenarios = Table(
      ("Spec", "Field", "Bad Value"),
      ("customer ID is invalid", "epp_unique_customer_id", "I am a bad customer ID."),
      ("registration ref is invalid", "epp_reg_reference", "I am a bad registration reference"),
      ("payment ref is invalid", "outbound_child_payment_ref", "I am a bad payment reference.")
    )

    forAll(endpoints) { (name, tfc_url, nsi_url, validPayload) =>
      s"POST $tfc_url" should {
        s"respond with $BAD_REQUEST and generic error message" when {
          forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
            val expectedCorrelationId = UUID.randomUUID().toString

            spec in withAuthNinoRetrievalExpectLog(name, expectedCorrelationId) {
              val makePaymentRequest = randomPaymentRequestJson + (field, JsString(badValue))

              val res = wsClient
                .url(s"$baseUrl$tfc_url")
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

        s"respond with status $UNAUTHORIZED" when {
          s"POST /auth/authorise responds $UNAUTHORIZED" in {
            stubFor(
              post("/auth/authorise") willReturn unauthorized()
            )

            val response = wsClient
              .url(s"$baseUrl$tfc_url")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> UUID.randomUUID().toString
              )
              .post(validPayload)
              .futureValue

            response.status shouldBe UNAUTHORIZED
            response.json shouldBe Json.obj(
              "errorCode"        -> "UNAUTHORISED",
              "errorDescription" -> "Invalid authentication credentials"
            )
          }
        }

        forAll(nsiErrorScenarios) {
          (nsiStatusCode, nsiErrorCode, expectedUpstreamStatusCode, expectedErrorCode, expectedErrorDescription) =>
            s"respond with status $expectedUpstreamStatusCode, errorCode $expectedErrorCode, and errorDescription \"$expectedErrorDescription\"" when {
              s"NSI responds status code $nsiStatusCode and errorCode $nsiErrorCode" in withAuthNinoRetrieval {
                val nsiResponseBody = Json.obj("errorCode" -> nsiErrorCode)
                val nsiResponse     = aResponse().withStatus(nsiStatusCode).withBody(nsiResponseBody.toString)
                stubFor(post(nsi_url) willReturn nsiResponse)

                val response = wsClient
                  .url(s"$baseUrl$tfc_url")
                  .withHttpHeaders(
                    AUTHORIZATION  -> "Bearer qwertyuiop",
                    CORRELATION_ID -> UUID.randomUUID().toString
                  )
                  .post(validPayload)
                  .futureValue

                response.status shouldBe expectedUpstreamStatusCode
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
