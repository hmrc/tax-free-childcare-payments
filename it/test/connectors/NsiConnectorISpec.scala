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

package connectors

import helpers.{BaseISpec, NsiStubs}
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import config.AppConfig
import helpers.generators.IdentifierRequestGenerators
import helpers.generators.request.external.ExternalLinkRequestGenerators
import helpers.generators.response.error.NsiErrorResponseGenerators
import helpers.generators.response.nsi.{NsiBalanceResponseGenerators, NsiLinkResponseGenerators, NsiPaymentResponseGenerators}
import models.request.IdentifierRequest
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import models.response.error.NsiErrorResponse.{E0001, E0009, E0024, E0027}
import models.response.error.ServiceErrorResponse.{ETFC3, ETFC4}
import models.response.nsi.{NsiBalanceResponse, NsiLinkResponse, NsiPaymentResponse}
import org.mockito.Mockito
import org.mockito.Mockito.{spy, when}
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class NsiConnectorISpec
    extends BaseISpec
    with NsiStubs
    with EitherValues
    with BeforeAndAfterEach
    with IdentifierRequestGenerators
    with ExternalLinkRequestGenerators
    with NsiLinkResponseGenerators
    with NsiBalanceResponseGenerators
    with NsiPaymentResponseGenerators
    with NsiErrorResponseGenerators {

  private val httpClientV2         = app.injector.instanceOf[HttpClientV2]
  private val appConfig: AppConfig = spy(app.injector.instanceOf[AppConfig])

  private val connector = new NsiConnector(httpClientV2, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    Mockito.reset(appConfig)
  }

  "method linkAccounts" should {
    "return Right LinkResponse" when {
      "NSI responds 201 with expected JSON format" in
        forAll(genLinkIdentifierRequests, genNsiLinkResponses) {
          (request: IdentifierRequest[ExternalLinkRequest], expectedNsiResponse: NsiLinkResponse) =>
            stubNsiLinkAccounts(status = CREATED, body = Json.toJson(expectedNsiResponse).toString)

            val expectedExternalResponse = expectedNsiResponse.toExternalLinkResponse

            val actualResponse = connector.linkAccounts(using request).futureValue.value

            actualResponse shouldBe expectedExternalResponse

            WireMock.verify(
              getRequestedFor(nsiLinkAccountsUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
            )
        }
    }

    "return Left E0001 and log errorDescription" when {
      "NSI responds with error status, errorCode E0001, and defined errorDescription" in
        forAll(
          genLinkIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0001", expectedErrorDescription)

            stubNsiLinkAccounts(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0001

            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $errorJson - triggering E0001"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left E0024 and log errorDescription" when {
      "NSI responds with error status, errorCode E0024, and defined errorDescription" in
        forAll(
          genLinkIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0024", expectedErrorDescription)

            stubNsiLinkAccounts(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0024

            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $errorJson - triggering E0024"
            checkLoneLog(
              expectedLevel = Level.INFO,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          genLinkIdentifierRequests,
          genInvalidNsiErrorResponseStrings
        ) { (request, unknownErrorCode) =>
          stubNsiLinkAccounts(status = BAD_REQUEST, body = errorAsJson(unknownErrorCode, "An error occurred").toString)

          val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }

    "return failed Future" when {
      "the request to NSI times out" in
        forAll(genLinkIdentifierRequests, genNsiLinkResponses) {
          (request: IdentifierRequest[ExternalLinkRequest], expectedResponse: NsiLinkResponse) =>
            when(appConfig.nsiRequestTimeout).thenReturn(100.millis)

            stubNsiLinkAccounts(
              status = CREATED,
              body = Json.toJson(expectedResponse).toString,
              responseTime = 200.millis
            )

            val actualResponse = connector.linkAccounts(using request).failed.futureValue

            actualResponse shouldBe a[GatewayTimeoutException]
            actualResponse.getMessage should include(
              s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms"
            )
        }
    }
  }

  "method checkBalance" should {
    "return Right(BalanceResponse)" when {
      s"NSI responds $OK with expected JSON format" in
        forAll(genBalanceIdentifierRequests, genNsiBalanceResponses) {
          (request: IdentifierRequest[ExternalBalanceRequest], expectedResponse: NsiBalanceResponse) =>
            stubNsiBalanceCheck(status = OK, body = Json.toJson(expectedResponse).toString)

            val actualResponse = connector.checkBalance(using request).futureValue.value

            actualResponse shouldBe expectedResponse.toExternalBalanceResponse

            WireMock.verify(
              getRequestedFor(nsiBalanceUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
            )
        }
    }

    "return Left ETFC3" when {
      "NSI responds with an invalid account status" in
        forAll(genBalanceIdentifierRequests) { externalBalanceRequest =>
          given IdentifierRequest[ExternalBalanceRequest] = externalBalanceRequest

          val invalidBalanceResponse = Json.obj(
            "accountStatus"  -> "unknown",
            "topUpAvailable" -> 1234,
            "topUpRemaining" -> 1234,
            "paidIn"         -> 1234,
            "totalBalance"   -> 1234,
            "clearedFunds"   -> 1234
          )

          stubFor {
            nsiCheckBalanceEndpoint
              .withQueryParams(nsiBalanceUrlQueryParams)
              .willReturn(created().withBody(invalidBalanceResponse.toString))
          }

          val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          genBalanceIdentifierRequests,
          genInvalidNsiErrorResponseStrings
        ) { (request, unknownErrorCode) =>
          stubNsiBalanceCheck(status = BAD_REQUEST, body = errorAsJson(unknownErrorCode, "An error occurred").toString)

          val actualNsiErrorResponse = connector.checkBalance(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }

    "return failed Future" when {
      "the request to NSI times out" in
        forAll(genBalanceIdentifierRequests, genNsiBalanceResponses) {
          (request: IdentifierRequest[ExternalBalanceRequest], expectedResponse: NsiBalanceResponse) =>
            when(appConfig.nsiRequestTimeout).thenReturn(100.millis)
            stubNsiBalanceCheck(status = OK, body = Json.toJson(expectedResponse).toString, responseTime = 200.millis)

            val actualResponse = connector.checkBalance(using request).failed.futureValue

            actualResponse shouldBe a[GatewayTimeoutException]
            actualResponse.getMessage should include(
              s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms"
            ).or(
              include(
                s"Request timeout to localhost:$wireMockPort after 100ms"
              )
            )
        }
    }
  }

  "method makePayment" should {
    "return Right NsiPaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in
        forAll(genPaymentIdentifierRequests, genNsiPaymentResponses) {
          (request: IdentifierRequest[ExternalPaymentRequest], expectedResponse: NsiPaymentResponse) =>
            stubNsiMakePayment(OK, Json.toJson(expectedResponse).toString)

            val actualResponse = connector.makePayment(using request).futureValue.value

            actualResponse shouldBe expectedResponse.toExternalPaymentResponse

            WireMock.verify(
              postRequestedFor(nsiPaymentUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
            )
        }
    }

    "return Left E0009 and log errorDescription" when {
      "NSI responds with error status, errorCode E0027, and defined errorDescription" in
        forAll(
          genPaymentIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0009", expectedErrorDescription)

            stubNsiMakePayment(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0009

            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $errorJson - triggering E0009"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left E0027 and log errorDescription" when {
      "NSI responds with error status, errorCode E0027, and defined errorDescription" in
        forAll(
          genPaymentIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0027", expectedErrorDescription)

            stubNsiMakePayment(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0027

            val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $errorJson - triggering E0027"

            checkLoneLog(
              expectedLevel = Level.INFO,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          genPaymentIdentifierRequests,
          genInvalidNsiErrorResponseStrings
        ) { (request, unknownErrorCode) =>
          stubNsiMakePayment(status = BAD_REQUEST, body = errorAsJson(unknownErrorCode, "An error occurred").toString)

          val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }

    "return failed Future" when {
      "the request to NSI times out" in
        forAll(genPaymentIdentifierRequests, genNsiPaymentResponses) {
          (request: IdentifierRequest[ExternalPaymentRequest], expectedResponse: NsiPaymentResponse) =>
            when(appConfig.nsiRequestTimeout).thenReturn(100.millis)
            stubNsiMakePayment(status = OK, body = Json.toJson(expectedResponse).toString, responseTime = 200.millis)

            val actualResponse = connector.makePayment(using request).failed.futureValue

            actualResponse shouldBe a[GatewayTimeoutException]
            actualResponse.getMessage should include(
              s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms"
            )
        }
    }
  }

  private def getFullLogMessageFrom(partialLogMessage: String) = s"[Error] - [ ] - [null: $partialLogMessage]"

  private val LOGGER = Logger(classOf[NsiConnector.type])
}
