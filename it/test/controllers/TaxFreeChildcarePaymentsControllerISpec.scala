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

import helpers.{AuthStubs, BaseISpec, NsiStubs}
import ch.qos.logback.classic.Level
import connectors.NsiConnector
import helpers.error.ExpectedErrorResponses
import helpers.generators.IdentifierRequestGenerators
import helpers.generators.other.NinoGenerators
import helpers.generators.request.external.{ExternalBalanceRequestGenerators, ExternalLinkRequestGenerators, ExternalPaymentRequestGenerators}
import helpers.generators.response.nsi.{NsiBalanceResponseGenerators, NsiLinkResponseGenerators, NsiPaymentResponseGenerators}
import org.scalatest.Assertion
import play.api.Logger
import play.api.libs.json.{JsPath, Json, JsonValidationError, KeyPathNode}
import play.api.libs.ws.{WSResponse, writeableOf_JsValue}

import java.util.UUID
import scala.util.matching.Regex

class TaxFreeChildcarePaymentsControllerISpec
    extends BaseISpec
    with AuthStubs
    with NsiStubs
    with ExternalLinkRequestGenerators
    with ExternalBalanceRequestGenerators
    with ExternalPaymentRequestGenerators
    with IdentifierRequestGenerators
    with NsiLinkResponseGenerators
    with NsiBalanceResponseGenerators
    with NsiPaymentResponseGenerators
    with ExpectedErrorResponses
    with NinoGenerators {

  import org.scalacheck.{Arbitrary, Gen}
  import Arbitrary.arbitrary

  private val LINK_URL    = s"$baseUrl/link"
  private val BALANCE_URL = s"$baseUrl/balance"
  private val PAYMENT_URL = s"$baseUrl/"

  private val endpoints = Table(
    ("Name", "TFC URL", "Valid Payload"),
    ("link", "/link", genExternalLinkRequestJsObjects.sample.get),
    ("balance", "/balance", genExternalLinkRequestJsObjects.sample.get),
    ("payment", "/", genExternalPaymentRequestJsObjects.sample.get)
  )

  private val CONTROLLER_LOGGER = Logger(classOf[TaxFreeChildcarePaymentsController])

  private val NSI_CONNECTOR_LOGGER = Logger(classOf[NsiConnector])

  private val EXPECTED_LOG_MESSAGE_PATTERN: Regex =
    raw"^\[Error] - \[([^]]+)] - \[([^:]+): (.+)]$$".r

  private val nsiErrorScenarios = Table(
    ("NSI Status Code", "NSI Error Code", "Expected Status Code", "Expected Error Description"),
    (BAD_REQUEST, "E0000", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0001", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0002", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0003", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0004", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0005", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0006", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0007", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0008", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0020", BAD_GATEWAY, EXPECTED_502_DESC),
    (BAD_REQUEST, "E0021", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0022", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0023", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (BAD_REQUEST, "E0024", BAD_REQUEST, EXPECTED_E0024_DESC),
    (BAD_REQUEST, "E0025", BAD_REQUEST, EXPECTED_E0025_DESC),
    (BAD_REQUEST, "E0026", BAD_REQUEST, EXPECTED_E0026_DESC),
    (BAD_REQUEST, "E0027", BAD_REQUEST, EXPECTED_E0027_DESC),
    (UNAUTHORIZED, "E0401", INTERNAL_SERVER_ERROR, EXPECTED_500_DESC),
    (FORBIDDEN, "E0030", BAD_REQUEST, EXPECTED_E0030_DESC),
    (FORBIDDEN, "E0031", BAD_REQUEST, EXPECTED_E0031_DESC),
    (FORBIDDEN, "E0032", BAD_REQUEST, EXPECTED_E0032_DESC),
    (FORBIDDEN, "E0033", BAD_REQUEST, EXPECTED_E0033_DESC),
    (FORBIDDEN, "E0034", SERVICE_UNAVAILABLE, EXPECTED_503_DESC),
    (FORBIDDEN, "E0035", BAD_REQUEST, EXPECTED_E0035_DESC),
    (FORBIDDEN, "E0036", BAD_REQUEST, EXPECTED_E0036_DESC),
    (NOT_FOUND, "E0042", BAD_REQUEST, EXPECTED_E0042_DESC),
    (NOT_FOUND, "E0043", BAD_REQUEST, EXPECTED_E0043_DESC),
    (INTERNAL_SERVER_ERROR, "E9000", SERVICE_UNAVAILABLE, EXPECTED_503_DESC),
    (INTERNAL_SERVER_ERROR, "E9999", SERVICE_UNAVAILABLE, EXPECTED_503_DESC),
    (SERVICE_UNAVAILABLE, "E8000", SERVICE_UNAVAILABLE, EXPECTED_503_DESC),
    (SERVICE_UNAVAILABLE, "E8001", SERVICE_UNAVAILABLE, EXPECTED_503_DESC)
  )

  "POST /link" should {

    "respond with status 200 and correct JSON body" when {
      "link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
        forAll(genLinkIdentifierRequests, genNsiLinkResponses) { (request, nsiLinkResponse) =>
          stubAuthRetrievalOf(request.nino)

          val nsiLinkResponseJson = Json.toJson(nsiLinkResponse)

          stubNsiLinkAccounts(CREATED, nsiLinkResponseJson.toString)

          val expectedCorrelationId    = request.correlation_id.toString
          val externalLinkResponse     = nsiLinkResponse.toExternalLinkResponse
          val externalLinkResponseJson = Json.toJson(externalLinkResponse)

          withClient { wsClient =>
            val wsResponse = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(Json.toJson(request.body))
              .futureValue

            wsResponse.status shouldBe OK
            wsResponse.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            wsResponse.json shouldBe externalLinkResponseJson
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"outbound_child_payment_ref is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(genIdentifierRequests(genExternalLinkRequestJsObjectsWithoutOutboundChildPaymentRef)) { request =>
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

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }

      "TFC account ref is invalid" in
        forAll(genIdentifierRequests(genExternalLinkRequestJsObjectsWithInvalidOutboundChildPaymentRef)) { request =>
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

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }
    }

    "respond 400 with errorCode E0006 and expected errorDescription" when {
      val expectedErrorDesc = s"child_date_of_birth is in invalid format or missing"

      "child DoB is missing" in
        forAll(genIdentifierRequests(genExternalLinkRequestJsObjectsWithoutChildDateOfBirth)) { request =>
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

              (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0006", expectedErrorDesc))
            }
          }
        }

      "child DoB is not a string" in
        forAll(genIdentifierRequests(genExternalLinkRequestJsObjectsWithNonStringChildDateOfBirth)) { request =>
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

              (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0006", expectedErrorDesc))
            }
          }
        }

      "child DoB is not ISO 8061" in
        forAll(genIdentifierRequests(genExternalLinkRequestJsObjectsWithNonIso8601ChildDateOfBirth)) { request =>
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

              (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0006", expectedErrorDesc))
            }
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = genLinkIdentifierRequests.sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiLinkAccounts(nsiStatus, errorAsJson(nsiErrorCode, arbitrary[String].sample.get).toString)

          withClient { wsClient =>
            val response = wsClient
              .url(LINK_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(Json.toJson(request.body))
              .futureValue

            (response.status, response.json) shouldBe (expectedApiStatus, errorAsJson(nsiErrorCode, expectedApiErrorDesc))
          }
      }
    }
  }

  "POST /balance" should {

    s"respond with status 200 and correct JSON body" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
        forAll(genBalanceIdentifierRequests, genNsiBalanceResponses) { (request, nsiBalanceResponse) =>
          withClient { wsClient =>
            val expectedCorrelationId = request.correlation_id.toString

            val responseJson = Json.toJson(nsiBalanceResponse)

            stubAuthRetrievalOf(request.nino)
            stubNsiBalanceCheck(OK, responseJson.toString)

            val res = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(Json.toJson(request.body))
              .futureValue

            res.status shouldBe OK
            res.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            res.json shouldBe Json.toJson(nsiBalanceResponse.toExternalBalanceResponse)
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"outbound_child_payment_ref is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(genIdentifierRequests(genExternalBalanceRequestJsObjectsWithoutOutboundChildPaymentRef)) { request =>
          withClient { wsClient =>
            stubAuthRetrievalOf(request.nino)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }

      "TFC account ref is invalid" in
        forAll(genIdentifierRequests(genExternalBalanceRequestJsObjectsWithInvalidOutboundChildPaymentRef)) { request =>
          withClient { wsClient =>
            stubAuthRetrievalOf(request.nino)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = genBalanceIdentifierRequests.sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiBalanceCheck(
            status = nsiStatus,
            body = errorAsJson(nsiErrorCode, arbitrary[String].sample.get).toString
          )

          withClient { wsClient =>
            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(Json.toJson(request.body))
              .futureValue

            (response.status, response.json) shouldBe (expectedApiStatus, errorAsJson(nsiErrorCode, expectedApiErrorDesc))
          }
      }
    }

    "respond with status 502, errorCode ETFC3" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK with unknown account status" in
        withCaptureOfLoggingFrom(NSI_CONNECTOR_LOGGER) { logs =>
          withClient { wsClient =>
            val expectedCorrelationId = UUID.randomUUID()
            val expectedNsiResponseBody = Json.obj(
              "accountStatus"  -> "UNKNOWN",
              "topUpAvailable" -> 0,
              "topUpRemaining" -> 0,
              "paidIn"         -> 0,
              "totalBalance"   -> 0,
              "clearedFunds"   -> 0
            )

            stubAuthRetrievalOf(genNinos.sample.get)
            stubNsiBalanceCheck(OK, expectedNsiResponseBody.toString)

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(Json.toJson(genExternalBalanceRequests.sample.get))
              .futureValue

            val expectedJsonErrors = List(
              JsPath(List(KeyPathNode("accountStatus"))) -> List(JsonValidationError("error.invalid.account_status"))
            )
            val expectedPartialMessage =
              s"NSI responded 200. Resulting in JSON validation errors - $expectedJsonErrors - triggering ETFC3"
            val expectedLogMessage = s"[Error] - [balance] - [$expectedCorrelationId: $expectedPartialMessage]"
            checkLoneLog(Level.WARN, expectedLogMessage)(logs)

            checkErrorResponse(response, BAD_GATEWAY, "ETFC3", "Bad Gateway")
          }
        }
    }

    "respond with status 502, errorCode ETFC4" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds with unknown errorCode" in
        withCaptureOfLoggingFrom(NSI_CONNECTOR_LOGGER) { logs =>
          withClient { wsClient =>
            stubAuthRetrievalOf(genNinos.sample.get)

            val errorJson = errorAsJson("Unknown", "A server error occurred")

            stubNsiBalanceCheck(INTERNAL_SERVER_ERROR, errorJson.toString)

            val expectedCorrelationId = UUID.randomUUID()

            val response = wsClient
              .url(BALANCE_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
              .post(Json.toJson(genExternalBalanceRequests.sample.get))
              .futureValue

            val expectedPartialMessage = s"NSI responded 500 with body $errorJson - triggering ETFC4"
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
          genIdentifierRequests(genExternalPaymentRequests),
          genNsiPaymentResponses
        ) { (request, nsiPaymentResponse) =>
          stubAuthRetrievalOf(request.nino)

          val nsiPaymentResponseJson = Json.toJson(nsiPaymentResponse)

          stubNsiMakePayment(status = OK, body = nsiPaymentResponseJson.toString)

          withClient { ws =>
            val expectedCorrelationId   = request.correlation_id.toString
            val expectedTfcResponseBody = Json.toJson(nsiPaymentResponse.toExternalPaymentResponse)

            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(Json.toJson(request.body))
              .futureValue

            response.status shouldBe OK
            response.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            response.json shouldBe expectedTfcResponseBody
          }
        }
    }

    "respond 400 with errorCode E0001 and expected errorDescription" when {
      val expectedErrorDesc = s"outbound_child_payment_ref is in invalid format or missing"

      "TFC account ref is missing" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithoutOutboundChildPaymentRef)) { request =>
          withClient { wsClient =>
            stubAuthRetrievalOf(request.nino)

            val response = wsClient
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }

      "TFC account ref is invalid" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithInvalidOutboundChildPaymentRef)) { request =>
          withClient { wsClient =>
            stubAuthRetrievalOf(request.nino)

            val response = wsClient
              .url(s"$baseUrl/")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0001", expectedErrorDesc))
          }
        }
    }

    "respond 400 with errorCode E0007 and expected errorDescription" when {
      val expectedErrorDesc = s"payee_type is in invalid format or missing"

      "payee type is missing" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithoutPayeeType)) { request =>
          stubAuthRetrievalOf(request.nino)

          withClient { ws =>
            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0007", expectedErrorDesc))
          }
        }

      "payee type is invalid" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithInvalidPayeeType)) { request =>
          stubAuthRetrievalOf(request.nino)

          withClient { ws =>
            val response = ws
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(request.body)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("E0007", expectedErrorDesc))
          }
        }
    }

    "respond 400 with E0008 and expected errorDescription" when {
      val expectedErrorDesc = s"payment_amount is in invalid format or missing"

      "payment amount is fractional" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithFractionalPaymentAmount)) { request =>
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

              (res.status, res.json) shouldBe (BAD_REQUEST, errorAsJson("E0008", expectedErrorDesc))
            }
          }
        }

      "payment amount is a string" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithStringPaymentAmount)) { request =>
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

              (res.status, res.json) shouldBe (BAD_REQUEST, errorAsJson("E0008", expectedErrorDesc))
            }
          }
        }

      "payment amount is non-positive" in
        forAll(genIdentifierRequests(genExternalPaymentRequestJsObjectsWithNegativePaymentAmount)) { request =>
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

              (res.status, res.json) shouldBe (BAD_REQUEST, errorAsJson("E0008", expectedErrorDesc))
            }
          }
        }
    }

    "response with expected status, errorCode, & errorDesc" when {
      "NSI responds with given error" in forAll(nsiErrorScenarios) {
        (nsiStatus, nsiErrorCode, expectedApiStatus, expectedApiErrorDesc) =>
          val request = genIdentifierRequests(genExternalPaymentRequests).sample.get

          stubAuthRetrievalOf(request.nino)
          stubNsiMakePayment(
            status = nsiStatus,
            body = errorAsJson(nsiErrorCode, arbitrary[String].sample.get).toString
          )

          withClient { wsClient =>
            val response = wsClient
              .url(PAYMENT_URL)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> request.correlation_id.toString
              )
              .post(Json.toJson(request.body))
              .futureValue

            (response.status, response.json) shouldBe (expectedApiStatus, errorAsJson(nsiErrorCode, expectedApiErrorDesc))
          }
      }
    }
  }

  forAll(endpoints) { (_, tfc_url, validPayload) =>
    s"POST $tfc_url" should {
      "respond 400 with errorCode ETFC1 and expected errorDescription" when {
        "correlation ID is missing" in
          withClient { ws =>
            stubAuthRetrievalOf(genNinos.sample.get)

            val response = ws
              .url(s"$baseUrl$tfc_url")
              .withHttpHeaders(
                AUTHORIZATION -> "Bearer qwertyuiop"
              )
              .post(validPayload)
              .futureValue

            (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("ETFC1", EXPECTED_ETFC1_MISSING_OR_INVALID_CORRELATION_ID_DESC))
          }

        "correlation ID is invalid" in
          forAll(Gen.alphaNumStr) { invalid_uuid =>
            stubAuthRetrievalOf(genNinos.sample.get)

            withClient { ws =>
              val response = ws
                .url(s"$baseUrl$tfc_url")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalid_uuid
                )
                .post(validPayload)
                .futureValue

              (response.status, response.json) shouldBe (BAD_REQUEST, errorAsJson("ETFC1", EXPECTED_ETFC1_MISSING_OR_INVALID_CORRELATION_ID_DESC))
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

          (response.status, response.json) shouldBe (INTERNAL_SERVER_ERROR, errorAsJson("ETFC2", EXPECTED_ETFC2_NO_NINO_RETRIEVED_DESC))
        }
      }

      "return a 401 response and expected errorDescription" when {
        "confidence level is insufficient" in {
          stubAuthWithLowConfidenceLevel

          withClient { ws =>
            val response = ws
              .url(s"$baseUrl$tfc_url")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> UUID.randomUUID().toString
              )
              .post(validPayload)
              .futureValue

            response.status shouldBe UNAUTHORIZED
            (response.json \ "statusCode").as[Int] shouldBe UNAUTHORIZED
            (response.json \ "message").as[String] shouldBe EXPECTED_INSUFFICIENT_CONFIDENCE_LEVEL_DESC
          }
        }
      }
    }
  }

  private def checkErrorResponse(
      actualResponse: WSResponse,
      expectedStatus: Int,
      expectedErrorCode: String,
      expectedErrorDescription: String
  ) =
    (actualResponse.status, actualResponse.json) shouldBe (
      expectedStatus,
      errorAsJson(expectedErrorCode, expectedErrorDescription)
    )

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
        loggedEndpoint shouldBe expectedEndpoint
        loggedCorrelationId shouldBe expectedCorrelationId
        loggedMessage should include("JsonValidationError")

      case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
    }
  }

}
