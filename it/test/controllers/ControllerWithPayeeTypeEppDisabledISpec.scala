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

import base.{AuthStubs, BaseISpec, NsiStubs}
import ch.qos.logback.classic.Level
import config.SanitisedJsonErrorHandler
import connectors.NsiConnector
import models.request.LinkRequest.CHILD_DOB_KEY
import models.request.Payee.PAYEE_TYPE_KEY
import models.request.PaymentRequest.PAYMENT_AMOUNT_KEY
import models.request.SharedRequestData.TFC_ACCOUNT_REF_KEY
import models.request.data.Generators
import models.request.{IdentifierRequest, LinkRequest, SharedRequestData}
import models.response.{BalanceResponse, LinkResponse, PaymentResponse}
import org.scalatest.Assertion
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.json.{JsPath, Json, JsonValidationError, KeyPathNode}
import play.api.libs.ws.WSResponse

import java.util.UUID
import scala.util.matching.Regex

class ControllerWithPayeeTypeEppDisabledISpec
    extends BaseISpec
    with AuthStubs
    with NsiStubs
    with ContentTypes
    with Generators
    with models.response.Generators {
  import org.scalacheck.{Arbitrary, Gen}
  import Arbitrary.arbitrary

  private val LINK_URL    = s"$baseUrl/link"
  private val BALANCE_URL = s"$baseUrl/balance"
  private val PAYMENT_URL = s"$baseUrl/"

  "POST /link" should {

    "respond with status 200 and correct JSON body" when {
      "link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
        forAll { (request: IdentifierRequest[LinkRequest], linkResponse: LinkResponse) =>
          stubAuthRetrievalOf(request.nino)
          stubNsiLinkAccounts201(getNsiJsonFrom(linkResponse))

          val expectedCorrelationId   = request.correlation_id.toString
          val expectedTfcResponseBody = Json.toJson(linkResponse)

          withClient { wsClient =>
            val wsResponse = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(getJsonFrom(request.body))
              .futureValue

            wsResponse.status                       shouldBe OK
            wsResponse.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            wsResponse.json                         shouldBe expectedTfcResponseBody
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"$TFC_ACCOUNT_REF_KEY is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(randomIdentifierRequest(linkPayloadsWithMissingTfcAccountRef)) { request =>
          stubAuthRetrievalOf(request.nino)

          withClient { wsClient =>
            val response = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }

      "TFC account ref is invalid" in
        forAll(randomIdentifierRequest(linkPayloadsWithInvalidTfcAccountRef)) { request =>
          stubAuthRetrievalOf(request.nino)

          withClient { wsClient =>
            val response = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }
    }

    "respond 400 with errorCode E0006 and expected errorDescription" when {
      val expectedErrorDesc = s"$CHILD_DOB_KEY is in invalid format or missing"

      "child DoB is missing" in
        forAll(randomIdentifierRequest(linkPayloadsWithMissingChildDob)) { request =>
          stubAuthRetrievalOf(request.nino)

          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("link", expectedCorrelationID) {
            withClient { wsClient =>
              val response = wsClient
                .url(LINK_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0006", expectedErrorDesc)
            }
          }
        }

      "child DoB is not a string" in
        forAll(randomIdentifierRequest(linkPayloadsWithNonStringChildDob)) { request =>
          stubAuthRetrievalOf(request.nino)

          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("link", expectedCorrelationID) {
            withClient { wsClient =>
              val response = wsClient
                .url(LINK_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0006", expectedErrorDesc)
            }
          }
        }

      "child DoB is not ISO 8061" in
        forAll(randomIdentifierRequest(linkPayloadsWithNonIso8061ChildDob)) { request =>
          stubAuthRetrievalOf(request.nino)

          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("link", expectedCorrelationID) {
            withClient { wsClient =>
              val response = wsClient
                .url(LINK_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0006", expectedErrorDesc)
            }
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = arbitrary[IdentifierRequest[LinkRequest]].sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiLinkAccountsError(nsiStatus, nsiErrorCode, arbitrary[String].sample.get)

          withClient { wsClient =>
            val response = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(getJsonFrom(request.body))
              .futureValue

            checkErrorResponse(response, expectedApiStatus, nsiErrorCode, expectedApiErrorDesc)
          }
      }
    }
  }

  "POST /balance" should {

    s"respond with status 200 and correct JSON body" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
        forAll { (request: IdentifierRequest[SharedRequestData], response: BalanceResponse) =>
          withClient { wsClient =>
            val expectedCorrelationId = request.correlation_id.toString

            stubAuthRetrievalOf(request.nino)
            stubNsiCheckBalance200(getNsiJsonFrom(response))

            val res = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(getJsonFrom(request.body))
              .futureValue

            res.status                       shouldBe OK
            res.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            res.json                         shouldBe Json.toJson(response)
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"$TFC_ACCOUNT_REF_KEY is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(Gen.uuid, randomNinos, sharedPayloadsWithMissingTfcAccountRef) { (expectedCorrelationId, nino, payload) =>
          withClient { wsClient =>
            stubAuthRetrievalOf(nino)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }

      "TFC account ref is invalid" in
        forAll(Gen.uuid, randomNinos, sharedPayloadsWithInvalidTfcAccountRef) { (expectedCorrelationId, nino, payload) =>
          withClient { wsClient =>
            stubAuthRetrievalOf(nino)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = arbitrary[IdentifierRequest[SharedRequestData]].sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiCheckBalanceError(nsiStatus, nsiErrorCode, arbitrary[String].sample.get)

          withClient { wsClient =>
            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(getJsonFrom(request.body))
              .futureValue

            checkErrorResponse(response, expectedApiStatus, nsiErrorCode, expectedApiErrorDesc)
          }
      }
    }

    "respond with status 502, errorCode ETFC3" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK with unknown account status" in
        withCaptureOfLoggingFrom(NSI_CONNECTOR_LOGGER) { logs =>
          withClient { wsClient =>
            val expectedCorrelationId   = UUID.randomUUID()
            val expectedNsiResponseBody = Json.obj(
              "accountStatus"  -> "UNKNOWN",
              "topUpAvailable" -> 0,
              "topUpRemaining" -> 0,
              "paidIn"         -> 0,
              "totalBalance"   -> 0,
              "clearedFunds"   -> 0
            )

            stubAuthRetrievalOf(randomNinos.sample.get)
            stubNsiCheckBalance200(expectedNsiResponseBody)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(validCheckBalanceRequestPayloads.sample.get)
              .futureValue

            val expectedJsonErrors     = List(JsPath(List(KeyPathNode("accountStatus"))) -> List(JsonValidationError("error.invalid.account_status")))
            val expectedPartialMessage = s"NSI responded 200. Resulted in JSON validation errors - $expectedJsonErrors - triggering ETFC3"
            val expectedLogMessage     = s"[Error] - [balance] - [$expectedCorrelationId: $expectedPartialMessage]"
            checkLoneLog(Level.WARN, expectedLogMessage)(logs)

            checkErrorResponse(response, BAD_GATEWAY, "ETFC3", "Bad Gateway")
          }
        }
    }

    "respond with status 502, errorCode ETFC4" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds with unknown errorCode" in
        withCaptureOfLoggingFrom(NSI_CONNECTOR_LOGGER) { logs =>
          withClient { wsClient =>
            stubAuthRetrievalOf(randomNinos.sample.get)
            stubNsiCheckBalanceError(INTERNAL_SERVER_ERROR, "Unknown", "A server error occurred")

            val expectedCorrelationId = UUID.randomUUID()

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(validCheckBalanceRequestPayloads.sample.get)
              .futureValue

            val expectedResponseJson   = Json.obj("errorCode" -> "Unknown", "errorDescription" -> "A server error occurred")
            val expectedPartialMessage = s"NSI responded 500 with body $expectedResponseJson - triggering ETFC4"
            val expectedLogMessage     = s"[Error] - [balance] - [$expectedCorrelationId: $expectedPartialMessage]"
            checkLoneLog(Level.WARN, expectedLogMessage)(logs)

            checkErrorResponse(response, BAD_GATEWAY, "ETFC4", "Bad Gateway")
          }
        }
    }
  }

  "POST /" should {

    "respond 200" when {
      "request is valid with payee type set to CCP" in
        forAll(
          randomIdentifierRequest(randomPaymentRequestWithOnlyCCP),
          arbitrary[PaymentResponse]
        ) { (request, expectedResponse) =>
          stubAuthRetrievalOf(request.nino)
          stubNsiMakePayment201(getNsiJsonFrom(expectedResponse))

          withClient { ws =>
            val expectedCorrelationId   = request.correlation_id.toString
            val expectedTfcResponseBody = Json.toJson(expectedResponse)

            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(getJsonFrom(request.body))
              .futureValue

            response.status                       shouldBe OK
            response.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            response.json                         shouldBe expectedTfcResponseBody
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"$TFC_ACCOUNT_REF_KEY is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(Gen.uuid, randomNinos, randomPaymentJsonWithCcpOnlyAndMissingTfcAccountRef) { (expectedCorrelationId, nino, payload) =>
          withClient { wsClient =>
            stubAuthRetrievalOf(nino)

            val response = wsClient
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }

      "TFC account ref is invalid" in
        forAll(Gen.uuid, randomNinos, randomPaymentJsonWithCcpOnlyAndInvalidTfcAccountRef) { (expectedCorrelationId, nino, payload) =>
          withClient { wsClient =>
            stubAuthRetrievalOf(nino)

            val response = wsClient
              .url(s"$baseUrl/")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0001", expectedErrorDesc)
          }
        }
    }

    "respond 400 with errorCode E0007 and expected errorDescription" when {
      val expectedErrorDesc = s"$PAYEE_TYPE_KEY is in invalid format or missing"

      "payee type is missing" in
        forAll(Gen.uuid, randomPaymentJsonWithMissingPayeeType) { (expectedCorrelationId, payload) =>
          stubAuthRetrievalOf(randomNinos.sample.get)

          withClient { ws =>
            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0007", expectedErrorDesc)
          }
        }

      "payee type is invalid" in
        forAll(Gen.uuid, randomPaymentJsonWithPayeeTypeNotCCP) { (expectedCorrelationId, payload) =>
          stubAuthRetrievalOf(randomNinos.sample.get)

          withClient { ws =>
            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(payload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "E0007", expectedErrorDesc)
          }
        }
    }

    "respond 400 with E0008 and expected errorDescription" when {
      val expectedErrorDesc = s"$PAYMENT_AMOUNT_KEY is in invalid format or missing"

      "payment amount is fractional" in
        forAll(randomIdentifierRequest(randomPaymentJsonWithCcpOnlyAndFractionalPaymentAmount)) { request =>
          stubAuthRetrievalOf(request.nino)
          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("payment", expectedCorrelationID) {
            withClient { ws =>
              val res = ws
                .url(PAYMENT_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0008", expectedErrorDesc)
            }
          }
        }

      "payment amount is a string" in
        forAll(randomIdentifierRequest(randomPaymentJsonWithCcpOnlyAndFractionalPaymentAmount)) { request =>
          stubAuthRetrievalOf(request.nino)
          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("payment", expectedCorrelationID) {
            withClient { ws =>
              val res = ws
                .url(PAYMENT_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0008", expectedErrorDesc)
            }
          }
        }

      "payment amount is non-positive" in
        forAll(randomIdentifierRequest(randomPaymentJsonWithCcpOnlyAndNonPositivePaymentAmount)) { request =>
          stubAuthRetrievalOf(request.nino)
          val expectedCorrelationID = request.correlation_id.toString

          expectLoneLog("payment", expectedCorrelationID) {
            withClient { ws =>
              val res = ws
                .url(PAYMENT_URL)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationID
                )
                .post(request.body)
                .futureValue

              checkErrorResponse(res, BAD_REQUEST, "E0008", expectedErrorDesc)
            }
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = randomIdentifierRequest(randomPaymentRequestWithOnlyCCP).sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiMakePaymentError(nsiStatus, nsiErrorCode, arbitrary[String].sample.get)

          withClient { wsClient =>
            val response = wsClient
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(getJsonFrom(request.body))
              .futureValue

            checkErrorResponse(response, expectedApiStatus, nsiErrorCode, expectedApiErrorDesc)
          }
      }
    }
  }

  private val endpoints = Table(
    ("Name",    "TFC URL",  "Valid Payload"),
    ("link",    "/link",    validLinkPayloads.sample.get),
    ("balance", "/balance", validSharedJson.sample.get),
    ("payment", "/",        validPaymentRequestWithPayeeTypeSetToCCP.sample.get)
  )

  private lazy val nsiErrorScenarios = Table(
    ("NSI Status Code",     "NSI Error Code", "Expected Status Code", "Expected Error Description"),
    (BAD_REQUEST,           "E0000",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0001",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0002",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0003",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0004",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0005",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0006",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0007",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0008",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0020",          BAD_GATEWAY,            EXPECTED_502_DESC),
    (BAD_REQUEST,           "E0021",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0022",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0023",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (BAD_REQUEST,           "E0024",          BAD_REQUEST,            EXPECTED_E0024_DESC),
    (BAD_REQUEST,           "E0025",          BAD_REQUEST,            EXPECTED_E0025_DESC),
    (BAD_REQUEST,           "E0026",          BAD_REQUEST,            EXPECTED_E0026_DESC),
    (BAD_REQUEST,           "E0027",          BAD_REQUEST,            EXPECTED_E0027_DESC),
    (UNAUTHORIZED,          "E0401",          INTERNAL_SERVER_ERROR,  EXPECTED_500_DESC),
    (FORBIDDEN,             "E0030",          BAD_REQUEST,            EXPECTED_E0030_DESC),
    (FORBIDDEN,             "E0031",          BAD_REQUEST,            EXPECTED_E0031_DESC),
    (FORBIDDEN,             "E0032",          BAD_REQUEST,            EXPECTED_E0032_DESC),
    (FORBIDDEN,             "E0033",          BAD_REQUEST,            EXPECTED_E0033_DESC),
    (FORBIDDEN,             "E0034",          SERVICE_UNAVAILABLE,    EXPECTED_503_DESC),
    (FORBIDDEN,             "E0035",          BAD_REQUEST,            EXPECTED_E0035_DESC),
    (FORBIDDEN,             "E0036",          BAD_REQUEST,            EXPECTED_E0036_DESC),
    (NOT_FOUND,             "E0042",          BAD_REQUEST,            EXPECTED_E0042_DESC),
    (NOT_FOUND,             "E0043",          BAD_REQUEST,            EXPECTED_E0043_DESC),
    (INTERNAL_SERVER_ERROR, "E9000",          SERVICE_UNAVAILABLE,    EXPECTED_503_DESC),
    (INTERNAL_SERVER_ERROR, "E9999",          SERVICE_UNAVAILABLE,    EXPECTED_503_DESC),
    (SERVICE_UNAVAILABLE,   "E8000",          SERVICE_UNAVAILABLE,    EXPECTED_503_DESC),
    (SERVICE_UNAVAILABLE,   "E8001",          SERVICE_UNAVAILABLE,    EXPECTED_503_DESC)
  )

  forAll(endpoints) { (endpointName, tfc_url, validPayload) =>
    s"POST $tfc_url" should {

      "respond 400 with errorCode E0000 and expected errorDescription" when {
        "request body can't be parsed to JSON" in
          withCaptureOfLoggingFrom(Logger(classOf[SanitisedJsonErrorHandler])) { logs =>
            withClient { ws =>
              val invalidJSON = "!@£$"

              val response = ws
                .url(s"$baseUrl$tfc_url")
                .withHttpHeaders(
                  CONTENT_TYPE -> JSON
                )
                .post(invalidJSON)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "E0000", "Invalid JSON")
            }
            val log = logs.loneElement
            log.getLevel shouldBe Level.INFO
            log.getMessage should startWith(s"[Error] - [$endpointName] - [null: Invalid JSON: ")
          }
      }

      "respond 400 with errorCode ETFC1 and expected errorDescription" when {
        "correlation ID is missing" in
          withClient { ws =>
            stubAuthRetrievalOf(randomNinos.sample.get)

            val response = ws
              .url(s"$baseUrl$tfc_url")
              .withHttpHeaders(
                AUTHORIZATION -> "Bearer qwertyuiop"
              )
              .post(validPayload)
              .futureValue

            checkErrorResponse(response, BAD_REQUEST, "ETFC1", EXPECTED_CORRELATION_ID_ERROR_DESC)
          }

        "correlation ID is invalid" in
          forAll(Gen.alphaNumStr) { invalid_uuid =>
            stubAuthRetrievalOf(randomNinos.sample.get)

            withClient { ws =>
              val response = ws
                .url(s"$baseUrl$tfc_url")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalid_uuid
                )
                .post(validPayload)
                .futureValue

              checkErrorResponse(response, BAD_REQUEST, "ETFC1", EXPECTED_CORRELATION_ID_ERROR_DESC)
            }
          }
      }

      "respond 500 with errorCode ETFC2 and expected errorDescription" when {
        "correlation ID is missing" in withClient { ws =>
          stubAuthEmptyRetrieval

          val response = ws
            .url(s"$baseUrl$tfc_url")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> UUID.randomUUID().toString
            )
            .post(validPayload)
            .futureValue

          checkErrorResponse(response, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_AUTH_NINO_RETRIEVAL_ERROR_DESC)
        }
      }
    }
  }

  private def checkErrorResponse(actualResponse: WSResponse, expectedStatus: Int, expectedErrorCode: String, expectedErrorDescription: String) = {
    actualResponse.status shouldBe expectedStatus
    checkErrorJson(actualResponse.json, expectedErrorCode, expectedErrorDescription)
  }

  private def expectLoneLog(
      expectedEndpoint: String,
      expectedCorrelationId: String
    )(
      doTest: => Assertion
    ): Unit = withCaptureOfLoggingFrom(CONTROLLER_LOGGER) { logs =>
    doTest

    val log = logs.loneElement
    log.getLevel shouldBe Level.INFO
    log.getMessage match {
      case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
        loggedEndpoint      shouldBe expectedEndpoint
        loggedCorrelationId shouldBe expectedCorrelationId
        loggedMessage         should include("JsonValidationError")

      case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
    }
  }

  private lazy val CONTROLLER_LOGGER = Logger(classOf[TaxFreeChildcarePaymentsController])

  private lazy val NSI_CONNECTOR_LOGGER = Logger(classOf[NsiConnector])

  private lazy val EXPECTED_LOG_MESSAGE_PATTERN: Regex =
    raw"^\[Error] - \[([^]]+)] - \[([^:]+): (.+)]$$".r
}
