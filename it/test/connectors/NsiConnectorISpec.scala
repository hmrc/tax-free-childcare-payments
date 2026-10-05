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

import base.{BaseISpec, NsiStubs}
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import config.AppConfig
import models.request.data.Generators
import models.request.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse.*
import models.response.{BalanceResponse, LinkResponse, NsiErrorResponse, PaymentResponse}
import org.mockito.Mockito
import org.mockito.Mockito.{spy, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class NsiConnectorISpec
    extends BaseISpec
    with NsiStubs
    with EitherValues
    with Generators
    with models.response.Generators
    with BeforeAndAfterEach {

  private val httpClientV2         = app.injector.instanceOf[HttpClientV2]
  private val appConfig: AppConfig = spy(app.injector.instanceOf[AppConfig])

  private val connector = new NsiConnector(httpClientV2, appConfig)
  
  private val testDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0)

  override def beforeEach(): Unit = {
    super.beforeEach()

    Mockito.reset(appConfig)
  }

  "method linkAccounts" should {
    "return Right LinkResponse" when {
      "NSI responds 201 with expected JSON format" in
        forAll { (request: IdentifierRequest[LinkRequest], expectedResponse: LinkResponse) =>
          stubNsiLinkAccounts201(getNsiJsonFrom(expectedResponse))

          val actualResponse = connector.linkAccounts(using request).futureValue.value

          actualResponse shouldBe expectedResponse
          WireMock.verify(
            getRequestedFor(nsiLinkAccountsUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
          )
        }
    }

    "return Left E0001 and log errorDescription" when {
      "NSI responds with error status, errorCode E0001, and defined errorDescription" in
        forAll(
          arbitrary[IdentifierRequest[LinkRequest]],
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get
            stubNsiLinkAccountsError(expectedStatus, "E0001", expectedErrorDescription)

            val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0001

            val expectedResponseJson = Json.obj("errorCode" -> "E0001", "errorDescription" -> expectedErrorDescription)
            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0001"
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
          arbitrary[IdentifierRequest[LinkRequest]],
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get
            stubNsiLinkAccountsError(expectedStatus, "E0024", expectedErrorDescription)

            val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0024

            val expectedResponseJson = Json.obj("errorCode" -> "E0024", "errorDescription" -> expectedErrorDescription)
            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0024"
            checkLoneLog(
              expectedLevel = Level.INFO,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC3" when {
      given Shrink[String] = Shrink.shrinkAny

      "NSI responds with an invalid account status" in
        forAll(randomNinos, Gen.uuid, validLinkRequestModels) { (nino, correlationId, linkRequest) =>
          given IdentifierRequest[LinkRequest] =
            IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), linkRequest))

          val invalidLinkResponse = Json.obj(
            "childFullName" -> "unknown"
          )

          stubFor {
            nsiMakePaymentEndpoint
              .willReturn(created().withBody(invalidLinkResponse.toString))
          }

          val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }
      
      "NSI responds with HTML Gateway Time-out" in
        forAll(
          arbitrary[IdentifierRequest[LinkRequest]]
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiLinkAccountsHtml(500, htmlError)

            val htmlErrorResponse = connector.linkAccounts(using request).futureValue.left.value

            htmlErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }


      "NSI responds with another exception" in
        forAll(
          arbitrary[IdentifierRequest[LinkRequest]]
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>

            stubNsiLinkAccountsException()

            val exceptionResponse = connector.linkAccounts(using request).futureValue.left.value

            exceptionResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"An exception occurred while reading NSI response triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          arbitrary[IdentifierRequest[LinkRequest]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiLinkAccountsError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.linkAccounts(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }

    "return failed Future" when {
      "the request to NSI times out" in
        forAll { (request: IdentifierRequest[LinkRequest], expectedResponse: LinkResponse) =>
          when(appConfig.nsiRequestTimeout).thenReturn(100.millis)
          stubNsiLinkAccounts201(getNsiJsonFrom(expectedResponse), 200.millis)

          val actualResponse = connector.linkAccounts(using request).failed.futureValue

          actualResponse shouldBe a[GatewayTimeoutException]
          actualResponse.getMessage should include(s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms")
        }
    }
  }

  "method checkBalance" should {
    "return Right(BalanceResponse)" when {
      s"NSI responds $OK with expected JSON format" in
        forAll { (request: IdentifierRequest[SharedRequestData], expectedResponse: BalanceResponse) =>
          stubNsiCheckBalance200(getNsiJsonFrom(expectedResponse))

          val actualResponse = connector.checkBalance(using request).futureValue.value

          actualResponse shouldBe expectedResponse
          WireMock.verify(
            getRequestedFor(nsiBalanceUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
          )
        }
    }

    "return Left ETFC3" when {
      given Shrink[String] = Shrink.shrinkAny

      "NSI responds with an invalid account status" in
        forAll(randomNinos, Gen.uuid, validSharedDataModels) { (nino, correlationId, sharedRequestData) =>
          given IdentifierRequest[SharedRequestData] =
            IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), sharedRequestData))

          val invalidBalanceResponse = Json.obj(
            "accountStatus" -> "unknown",
            "topUpAvailable" -> 1234,
            "topUpRemaining" -> 1234,
            "paidIn" -> 1234,
            "totalBalance" -> 1234,
            "clearedFunds" -> 1234
          )

          stubFor {
            nsiCheckBalanceEndpoint
              .withQueryParams(nsiBalanceUrlQueryParams)
              .willReturn(created().withBody(invalidBalanceResponse.toString))
          }

          val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }

      "NSI responds with HTML Gateway Time-out" in
        forAll(
          arbitrary[IdentifierRequest[SharedRequestData]]
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiCheckBalanceHtml(500, htmlError)

            val htmlErrorResponse = connector.checkBalance(using request).futureValue.left.value

            htmlErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with another exception" in
        forAll(
          arbitrary[IdentifierRequest[SharedRequestData]]
        ) { request =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>

              stubNsiCheckBalanceException()

              val exceptionResponse = connector.checkBalance(using request).futureValue.left.value

              exceptionResponse shouldBe ETFC3

              val expectedPartialLogMessage =
                s"An exception occurred while reading NSI response triggering ETFC3"
              checkLoneLog(
                expectedLevel = Level.WARN,
                expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
              )(logs)
            }
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          arbitrary[IdentifierRequest[SharedRequestData]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiCheckBalanceError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.checkBalance(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }

    "return failed Future" when {
      "the request to NSI times out" in
        forAll { (request: IdentifierRequest[SharedRequestData], expectedResponse: BalanceResponse) =>
          when(appConfig.nsiRequestTimeout).thenReturn(100.millis)
          stubNsiCheckBalance200(getNsiJsonFrom(expectedResponse), 200.millis)

          val actualResponse = connector.checkBalance(using request).failed.futureValue

          actualResponse shouldBe a[GatewayTimeoutException]
          actualResponse.getMessage should include(s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms")
        }
    }
  }

  "method makePayment" should {
    "return Right PaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in
        forAll { (request: IdentifierRequest[PaymentRequest], expectedResponse: PaymentResponse) =>
          stubNsiMakePayment201(getNsiJsonFrom(expectedResponse))

          val actualResponse = connector.makePayment(using request).futureValue.value

          actualResponse shouldBe expectedResponse
          WireMock.verify(
            postRequestedFor(nsiPaymentUrlPattern).withHeader(AUTHORIZATION, equalTo("Basic nsi-basic-token"))
          )
        }
    }

    "return Left E0009 and log errorDescription" when {
      "NSI responds with error status, errorCode E0027, and defined errorDescription" in
        forAll(
          arbitrary[IdentifierRequest[PaymentRequest]],
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get
            stubNsiMakePaymentError(expectedStatus, "E0009", expectedErrorDescription)

            val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0009

            val expectedResponseJson = Json.obj("errorCode" -> "E0009", "errorDescription" -> expectedErrorDescription)
            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0009"
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
          arbitrary[IdentifierRequest[PaymentRequest]],
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get
            stubNsiMakePaymentError(expectedStatus, "E0027", expectedErrorDescription)

            val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0027

            val expectedResponseJson = Json.obj("errorCode" -> "E0027", "errorDescription" -> expectedErrorDescription)
            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0027"
            checkLoneLog(
              expectedLevel = Level.INFO,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC3" when {
      given Shrink[String] = Shrink.shrinkAny

      "NSI responds with an invalid account status" in
        forAll(randomNinos, Gen.uuid, validPaymentRequestModels) { (nino, correlationId, paymentRequest) =>
          given IdentifierRequest[PaymentRequest] =
            IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), paymentRequest))

          val invalidPaymentResponse = Json.obj(
            "payment_reference"      -> "unknown", 
            "estimated_payment_date" -> testDateTime
          )

          stubFor {
            nsiMakePaymentEndpoint
              .willReturn(created().withBody(invalidPaymentResponse.toString))
          }

          val actualNsiErrorResponse = connector.makePayment.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }

      "NSI responds with HTML Gateway Time-out" in
        forAll(
          arbitrary[IdentifierRequest[PaymentRequest]]
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiMakePaymentHtml(500, htmlError)

            val htmlErrorResponse = connector.makePayment(using request).futureValue.left.value

            htmlErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with another exception" in
        forAll(
          arbitrary[IdentifierRequest[PaymentRequest]]
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>

            stubNsiMakePaymentException()

            val exceptionResponse = connector.makePayment(using request).futureValue.left.value

            exceptionResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"An exception occurred while reading NSI response triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          arbitrary[IdentifierRequest[PaymentRequest]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiMakePaymentError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.makePayment(using request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }


    "return failed Future" when {
      "the request to NSI times out" in
        forAll { (request: IdentifierRequest[PaymentRequest], expectedResponse: PaymentResponse) =>
          when(appConfig.nsiRequestTimeout).thenReturn(100.millis)
          stubNsiMakePayment201(getNsiJsonFrom(expectedResponse), 200.millis)

          val actualResponse = connector.makePayment(using request).failed.futureValue

          actualResponse shouldBe a[GatewayTimeoutException]
          actualResponse.getMessage should include(s"Request timeout to localhost/127.0.0.1:$wireMockPort after 100 ms")
        }
    }
  }

  private def getFullLogMessageFrom(partialLogMessage: String) = s"[Error] - [ ] - [null: $partialLogMessage]"

  private val LOGGER = Logger(classOf[NsiConnector.type])
}
