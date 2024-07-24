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

import base.{BaseISpec, NsiStubs}
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Assertion
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WSResponse

import java.util.UUID
import scala.util.matching.Regex

class TaxFreeChildcarePaymentsControllerISpec extends BaseISpec with NsiStubs with models.request.Generators {
  import org.scalacheck.Gen

  "POST /link" should {
    "respond with status 200 and correct JSON body" when {

      "link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
        forAll(validLinkPayloads) { payload =>
          withClient { wsClient =>
            withAuthNinoRetrieval {
              val expectedChildName       = fullNames.sample.get
              val expectedCorrelationId   = UUID.randomUUID()
              val expectedTfcResponseBody = Json.obj("child_full_name" -> expectedChildName)
              val expectedNsiResponseBody = Json.obj("childFullName" -> expectedChildName)

              stubNsiLinkAccounts201(expectedNsiResponseBody)

              val request  = wsClient
                .url(s"$baseUrl/link")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
              val response = request
                .post(payload)
                .futureValue

              val resCorrelationId = UUID fromString response.header(CORRELATION_ID).value

              response.status shouldBe OK
              resCorrelationId shouldBe expectedCorrelationId
              response.json shouldBe expectedTfcResponseBody
            }
          }
        }
    }

    "respond 400 with errorCode E0006 and expected errorDescription" when {
      val expectedCorrelationId = UUID.randomUUID()

      "child_date_of_birth is missing" in
        forAll(linkPayloadsWithMissingChildDob) { payload =>
          withClient { wsClient =>
            withAuthNinoRetrievalExpectLog("link", expectedCorrelationId.toString) {
              val response = wsClient
                .url(s"$baseUrl/link")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0006", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
    }

    "respond 400 with errorCode E0021 and expected errorDescription" when {
      val expectedCorrelationId = UUID.randomUUID()

      "child DOB is invalid" in forAll(linkPayloadsWithNonIso8061ChildDob) { payload =>
        withClient { wsClient =>
          val response = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(payload)
            .futureValue

          checkErrorResponse(response, BAD_REQUEST, "E0021", EXPECTED_400_ERROR_DESCRIPTION)
        }
      }
    }

    "respond 400 with errorCode E0024 and expected errorDescription" when {
      "NSI responds 400 with errorCode E0024" in
        forAll(Gen.uuid, validLinkPayloads) { (expectedCorrelationId, payload) =>
          stubFor {
            nsiLinkAccountsEndpoint willReturn badRequest().withBody(nsiJsonBody("E0024", Gen.asciiPrintableStr.sample.get))
          }

          withClient { wsClient =>
            withAuthNinoRetrieval {
              val response = wsClient
                .url(s"$baseUrl/link")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0024", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
    }

    "respond 400 with errorCode E0025 and expected errorDescription" when {
      "NSI responds 400 with errorCode E0025" in
        forAll(Gen.uuid, validLinkPayloads) { (expectedCorrelationId, payload) =>
          stubFor {
            nsiLinkAccountsEndpoint willReturn badRequest().withBody(nsiJsonBody("E0025", Gen.asciiPrintableStr.sample.get))
          }

          withClient { wsClient =>
            withAuthNinoRetrieval {
              val response = wsClient
                .url(s"$baseUrl/link")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0025", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
    }

    "respond 400 with errorCode E0026 and expected errorDescription" when {
      "NSI responds 400 with errorCode E0026" in
        forAll(Gen.uuid, validLinkPayloads) { (expectedCorrelationId, payload) =>
          stubFor {
            nsiLinkAccountsEndpoint willReturn badRequest().withBody(nsiJsonBody("E0026", Gen.asciiPrintableStr.sample.get))
          }

          withClient { wsClient =>
            withAuthNinoRetrieval {
              val response = wsClient
                .url(s"$baseUrl/link")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0026", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
    }
  }

  withClient { wsClient =>
    "POST /balance" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId   = UUID.randomUUID()
          val expectedTfcResponseBody = Json.obj(
            "tfc_account_status" -> "ACTIVE",
            "paid_in_by_you"     -> 0,
            "government_top_up"  -> 0,
            "total_balance"      -> 0,
            "cleared_funds"      -> 0,
            "top_up_allowance"   -> 0
          )
          val expectedNsiResponseBody = Json.obj(
            "accountStatus"  -> "ACTIVE",
            "topUpAvailable" -> 0,
            "topUpRemaining" -> 0,
            "paidIn"         -> 0,
            "totalBalance"   -> 0,
            "clearedFunds"   -> 0
          )

          stubNsiCheckBalance200(expectedNsiResponseBody)

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(validCheckBalanceRequestPayloads.sample.get)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedTfcResponseBody
        }
      }
    }
  }

  "POST /" should {
    "respond 200" when {
      "request is valid with payee type set to CCP" in withClient { ws =>
        withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomOutboundChildPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedTfcResponseBody = Json.obj(
            "payment_reference"      -> expectedPaymentRef,
            "estimated_payment_date" -> expectedPaymentDate
          )
          val expectedNsiResponseBody = Json.obj(
            "paymentReference" -> expectedPaymentRef,
            "paymentDate"      -> expectedPaymentDate
          )

          stubNsiMakePayment201(expectedNsiResponseBody)

          val res = ws
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(validPaymentRequestWithPayeeTypeSetToCCP.sample.get)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedTfcResponseBody
        }
      }
    }

    """respond 400 and errorMessage "payee_type did not match 'CCP'""""" when {
      "payee type is set to lowercase ccp" in withClient { ws =>
        withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomOutboundChildPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedNsiResponseBody = Json.obj(
            "paymentReference" -> expectedPaymentRef,
            "paymentDate"      -> expectedPaymentDate
          )

          stubNsiMakePayment201(expectedNsiResponseBody)

          val response = ws
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(validPaymentRequestWithPayeeTypeSetToccp.sample.get)
            .futureValue

          checkErrorResponse(response, BAD_REQUEST, "E0022", EXPECTED_400_ERROR_DESCRIPTION)
        }
      }

      "payee type is set to EPP" in withClient { ws =>
        withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomOutboundChildPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedNsiResponseBody = Json.obj(
            "paymentReference" -> expectedPaymentRef,
            "paymentDate"      -> expectedPaymentDate
          )

          stubNsiMakePayment201(expectedNsiResponseBody)

          val response = ws
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(validEppPaymentRequestWithPayeeTypeSetToEPP.sample.get)
            .futureValue

          checkErrorResponse(response, BAD_REQUEST, "E0022", EXPECTED_400_ERROR_DESCRIPTION)
        }
      }
    }

    "respond 400 with E0023" when {
      val expectedCorrelationId = UUID.randomUUID()

      "payment amount is fractional" in
        forAll(paymentPayloadsWithFractionalPaymentAmount) { payload =>
          withClient { ws =>
            withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
              val res = ws
                .url(s"$baseUrl/")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0023", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }

      "payment amount is a string" in
        forAll(paymentPayloadsWithStringPaymentAmount) { payload =>
          withClient { ws =>
            withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
              val res = ws
                .url(s"$baseUrl/")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0023", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }

      "payment amount is non-positive" in
        forAll(paymentPayloadsWithNonPositivePaymentAmount) { payload =>
          withClient { ws =>
            withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
              val res = ws
                .url(s"$baseUrl/")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId.toString
                )
                .post(payload)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0023", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
    }
  }

  private val endpoints = Table(
    ("Name", "TFC URL", "NSI Mapping", "Valid Payload"),
    ("link", "/link", nsiLinkAccountsEndpoint, validLinkPayloads.sample.get),
    ("balance", "/balance", nsiCheckBalanceEndpoint, validSharedPayloads.sample.get),
    ("payment", "/", nsiMakePaymentEndpoint, validPaymentRequestWithPayeeTypeSetToCCP.sample.get)
  )

  private val nsiErrorScenarios = Table(
    ("NSI Status Code", "NSI Error Code", "Expected Status Code"),
    (BAD_REQUEST, "E0000", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0001", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0002", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0003", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0004", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0005", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0006", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0007", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0008", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0020", BAD_GATEWAY),
    (BAD_REQUEST, "E0021", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0022", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0023", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0024", BAD_REQUEST),
    (BAD_REQUEST, "E0025", BAD_REQUEST),
    (BAD_REQUEST, "E0026", BAD_REQUEST),
    (UNAUTHORIZED, "E0401", INTERNAL_SERVER_ERROR),
    (FORBIDDEN, "E0030", BAD_REQUEST),
    (FORBIDDEN, "E0031", BAD_REQUEST),
    (FORBIDDEN, "E0032", BAD_REQUEST),
    (FORBIDDEN, "E0033", BAD_REQUEST),
    (FORBIDDEN, "E0034", SERVICE_UNAVAILABLE),
    (FORBIDDEN, "E0035", BAD_REQUEST),
    (NOT_FOUND, "E0040", BAD_REQUEST),
    (NOT_FOUND, "E0041", BAD_REQUEST),
    (NOT_FOUND, "E0042", BAD_REQUEST),
    (NOT_FOUND, "E0043", BAD_REQUEST),
    (INTERNAL_SERVER_ERROR, "E9000", SERVICE_UNAVAILABLE),
    (INTERNAL_SERVER_ERROR, "E9999", SERVICE_UNAVAILABLE),
    (SERVICE_UNAVAILABLE, "E8000", SERVICE_UNAVAILABLE),
    (SERVICE_UNAVAILABLE, "E8001", SERVICE_UNAVAILABLE)
  )

  private val sharedBadRequestScenarios = Table(
    ("Field", "Bad Value"),
    ("epp_unique_customer_id", "I am a bad customer ID."),
    ("epp_reg_reference", "I am a bad registration reference"),
    ("outbound_child_payment_ref", "I am a bad payment reference.")
  )

  forAll(endpoints) { (name, tfc_url, nsiMapping, validPayload) =>
    s"POST $tfc_url" should {
      "respond 400 with errorCode ETFC1 and expected errorDescription" when {
        "correlation ID is missing" in withClient { ws =>
          withAuthNinoRetrieval {
            val response = ws
              .url(s"$baseUrl$tfc_url")
              .withHttpHeaders(
                AUTHORIZATION -> "Bearer qwertyuiop"
              )
              .post(validPayload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION)
          }
        }

        "correlation ID is invalid" in withClient { ws =>
          forAll(Gen.alphaNumStr) { invalid_uuid =>
            withAuthNinoRetrieval {
              val response = ws
                .url(s"$baseUrl$tfc_url")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalid_uuid
                )
                .post(validPayload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION)
            }
          }
        }
      }

      "respond 500 with errorCode ETFC2 and expected errorDescription" when {
        "correlation ID is missing" in withClient { ws =>
          stubFor(post("/auth/authorise") willReturn okJson("{}"))

          val response = ws
            .url(s"$baseUrl$tfc_url")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> UUID.randomUUID().toString
            )
            .post(validPayload)
            .futureValue

          checkErrorResponse(response, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_500_ERROR_DESCRIPTION)
        }
      }

      "respond 400 with errorCode E0000 and errorDescription" when {
        "one of the shared JSON fields is invalid" in
          forAll(sharedBadRequestScenarios) { (field, badValue) =>
            val expectedCorrelationId = UUID.randomUUID().toString

            withClient { ws =>
              val invalidPayload = validPayload + (field -> JsString(badValue))

              withAuthNinoRetrievalExpectLog(name, expectedCorrelationId) {
                val response = ws
                  .url(s"$baseUrl$tfc_url")
                  .withHttpHeaders(
                    AUTHORIZATION  -> "Bearer qwertyuiop",
                    CORRELATION_ID -> expectedCorrelationId
                  )
                  .post(invalidPayload)
                  .futureValue

                response.status shouldBe BAD_REQUEST
                checkErrorJson(response.json, "E0000", EXPECTED_400_ERROR_DESCRIPTION)
              }
            }
          }
      }

      forAll(nsiErrorScenarios) {
        (nsiStatusCode, nsiErrorCode, expectedStatusCode) =>
          s"respond with status $expectedStatusCode" when {
            s"NSI responds status code $nsiStatusCode and errorCode $nsiErrorCode" in withClient { ws =>
              withAuthNinoRetrieval {
                val nsiResponseBody = Json.obj("errorCode" -> nsiErrorCode)
                val nsiResponse     = aResponse().withStatus(nsiStatusCode).withBody(nsiResponseBody.toString)
                stubFor(nsiMapping willReturn nsiResponse)

                val response = ws
                  .url(s"$baseUrl$tfc_url")
                  .withHttpHeaders(
                    AUTHORIZATION  -> "Bearer qwertyuiop",
                    CORRELATION_ID -> UUID.randomUUID().toString
                  )
                  .post(validPayload)
                  .futureValue

                response.status shouldBe expectedStatusCode
              }
            }
          }
      }
    }
  }

  private def checkErrorResponse(actualResponse: WSResponse, expectedStatus: Int, expectedErrorCode: String, expectedErrorDescription: String) = {
    actualResponse.status shouldBe expectedStatus
    checkErrorJson(actualResponse.json, expectedErrorCode, expectedErrorDescription)
  }

  private def withAuthNinoRetrievalExpectLog(
      expectedEndpoint: String,
      expectedCorrelationId: String
    )(
      doTest: => Assertion
    ): Unit = withCaptureOfLoggingFrom(EXPECTED_LOGGER) { logs =>
    withAuthNinoRetrieval {
      doTest
    }

    val log = logs.loneElement
    log.getLevel shouldBe Level.INFO
    log.getMessage match {
      case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
        loggedEndpoint shouldBe expectedEndpoint
        loggedCorrelationId shouldBe expectedCorrelationId
        loggedMessage should include("JsonValidationError")

      case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
    }
  }

  private lazy val EXPECTED_LOGGER = Logger(classOf[TaxFreeChildcarePaymentsController])

  private lazy val EXPECTED_LOG_MESSAGE_PATTERN: Regex =
    raw"^\[Error] - \[([^]]+)] - \[([^:]+): (.+)]$$".r
}
