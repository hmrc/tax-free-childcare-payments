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
import models.response.error.NsiErrorResponse.{E0001, E0006, E0009, E0024, E0027, E0032}
import models.response.error.ServiceErrorResponse.{ETFC3, ETFC4}
import models.response.nsi.{NsiBalanceResponse, NsiLinkResponse, NsiPaymentResponse}
import org.mockito.Mockito
import org.mockito.Mockito.{spy, when}
import org.scalacheck.{Gen, Shrink}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.LocalDateTime
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

  private val testDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0)

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

    "return Left ETFC3" when {
      "NSI responds 201 with an invalid JSON body" in
        forAll(genLinkIdentifierRequests) { externalLinkRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalLinkRequest] = externalLinkRequest

            val invalidLinkResponse = Json.obj(
              "childFullName" -> 123
            )

            stubFor {
              nsiLinkAccountsEndpoint
                .willReturn(created().withBody(invalidLinkResponse.toString))
            }

            val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded 201. Resulting in JSON validation errors - List((/childFullName,List(JsonValidationError(List(error.expected.jsstring),ArraySeq())))) - triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds 201 with a non-JSON body" in
        forAll(genLinkIdentifierRequests) { externalLinkRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalLinkRequest] = externalLinkRequest

            val invalidLinkResponse = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubFor {
              nsiLinkAccountsEndpoint
                .willReturn(created().withBody(invalidLinkResponse))
            }

            val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds 201 with an empty body" in
        forAll(genLinkIdentifierRequests) { externalLinkRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalLinkRequest] = externalLinkRequest

            val invalidLinkResponse = ""

            stubFor {
              nsiLinkAccountsEndpoint
                .willReturn(created().withBody(invalidLinkResponse))
            }

            val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"An exception occurred while reading NSI response triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with an error status and invalid JSON body" in
        forAll(genLinkIdentifierRequests) { externalLinkRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalLinkRequest] = externalLinkRequest

            val invalidLinkResponse = Json.obj(
              "childFullName" -> 123
            )

            stubFor {
              nsiLinkAccountsEndpoint
                .willReturn(aResponse().withStatus(BAD_REQUEST).withBody(invalidLinkResponse.toString))
            }

            val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded 400. Resulting in JSON validation errors - List((/errorCode,List(JsonValidationError(List(error.path.missing),ArraySeq())))) - triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with HTML Gateway Time-out" in
        forAll(
          genLinkIdentifierRequests
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiLinkAccountsHtml(504, htmlError)

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

      "NSI responds with another error" in
        forAll(
          genLinkIdentifierRequests
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>

            stubNsiLinkAccountsError()

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

    "return Left E0006 and log errorDescription" when {
      "NSI responds with error status, errorCode E0006, and defined errorDescription" in
        forAll(
          genBalanceIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0006", expectedErrorDescription)

            stubNsiBalanceCheck(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.checkBalance(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0006

            val expectedPartialLogMessage =
              s"NSI responded $expectedStatus with body $errorJson - triggering E0006"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left E0032 and log errorDescription" when {
      "NSI responds with error status, errorCode E0032, and defined errorDescription" in
        forAll(
          genBalanceIdentifierRequests,
          Gen.asciiPrintableStr
        ) { (request, expectedErrorDescription) =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val expectedStatus = randomHttpErrorCodes.sample.get

            val errorJson = errorAsJson("E0032", expectedErrorDescription)

            stubNsiBalanceCheck(status = expectedStatus, body = errorJson.toString)

            val actualNsiErrorResponse = connector.checkBalance(using request).futureValue.left.value

            actualNsiErrorResponse shouldBe E0032

            val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $errorJson - triggering E0032"

            checkLoneLog(
              expectedLevel = Level.INFO,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }
    }

    "return Left ETFC3" when {
      "NSI responds 201 with an invalid JSON body" in
        forAll(genBalanceIdentifierRequests) { externalBalanceRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
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

              val expectedPartialLogMessage =
                s"NSI responded 201. Resulting in JSON validation errors - List((/accountStatus,List(JsonValidationError(List(error.invalid.account_status),ArraySeq())))) - triggering ETFC3"
              checkLoneLog(
                expectedLevel = Level.WARN,
                expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
              )(logs)
          }
        }

      "NSI responds 201 with a non-JSON body" in
        forAll(genBalanceIdentifierRequests) { externalBalanceRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalBalanceRequest] = externalBalanceRequest

            val invalidBalanceResponse = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubFor {
              nsiCheckBalanceEndpoint
                .willReturn(created().withBody(invalidBalanceResponse))
            }

            val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds 201 with an empty body" in
        forAll(genBalanceIdentifierRequests) { externalBalanceRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalBalanceRequest] = externalBalanceRequest

            val invalidBalanceResponse = ""

            stubFor {
              nsiCheckBalanceEndpoint
                .willReturn(created().withBody(invalidBalanceResponse))
            }

            val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"An exception occurred while reading NSI response triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with an error status and invalid JSON body" in
        forAll(genBalanceIdentifierRequests) { externalBalanceRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalBalanceRequest] = externalBalanceRequest

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
                .willReturn(aResponse().withStatus(BAD_REQUEST).withBody(invalidBalanceResponse.toString))
            }

            val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded 400. Resulting in JSON validation errors - List((/errorCode,List(JsonValidationError(List(error.path.missing),ArraySeq())))) - triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with HTML Gateway Time-out" in
        forAll(
          genBalanceIdentifierRequests
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiCheckBalanceHtml(504, htmlError)

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

      "NSI responds with another error" in
        forAll(
          genBalanceIdentifierRequests
        ) { request =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>

              stubNsiCheckBalanceError()

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
      "NSI responds with error status, errorCode E0009, and defined errorDescription" in
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

    "return Left ETFC3" when {
      "NSI responds 201 with an invalid JSON body" in
        forAll(genPaymentIdentifierRequests) { externalPaymentRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalPaymentRequest] = externalPaymentRequest

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

            val expectedPartialLogMessage =
              s"NSI responded 201. Resulting in JSON validation errors - List((/paymentDate,List(JsonValidationError(List(error.path.missing),ArraySeq()))), (/paymentReference,List(JsonValidationError(List(error.path.missing),ArraySeq())))) - triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds 201 with a non-JSON body" in
        forAll(genPaymentIdentifierRequests) { externalPaymentRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalPaymentRequest] = externalPaymentRequest

            val invalidPaymentResponse = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubFor {
              nsiMakePaymentEndpoint
                .willReturn(created().withBody(invalidPaymentResponse))
            }

            val actualNsiErrorResponse = connector.makePayment.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded with a body that cannot be parsed triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds 201 with an empty body" in
        forAll(genPaymentIdentifierRequests) { externalPaymentRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalPaymentRequest] = externalPaymentRequest

            val invalidPaymentResponse = ""

            stubFor {
              nsiMakePaymentEndpoint
                .willReturn(created().withBody(invalidPaymentResponse))
            }

            val actualNsiErrorResponse = connector.makePayment.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"An exception occurred while reading NSI response triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with an error status and invalid JSON body" in
        forAll(genPaymentIdentifierRequests) { externalPaymentRequest =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            given IdentifierRequest[ExternalPaymentRequest] = externalPaymentRequest

            val invalidPaymentResponse = Json.obj(
              "payment_reference"      -> "unknown",
              "estimated_payment_date" -> testDateTime
            )

            stubFor {
              nsiMakePaymentEndpoint
                .willReturn(aResponse().withStatus(BAD_REQUEST).withBody(invalidPaymentResponse.toString))
            }

            val actualNsiErrorResponse = connector.makePayment.futureValue.left.value

            actualNsiErrorResponse shouldBe ETFC3

            val expectedPartialLogMessage =
              s"NSI responded 400. Resulting in JSON validation errors - List((/errorCode,List(JsonValidationError(List(error.path.missing),ArraySeq())))) - triggering ETFC3"
            checkLoneLog(
              expectedLevel = Level.WARN,
              expectedMessage = getFullLogMessageFrom(expectedPartialLogMessage)
            )(logs)
          }
        }

      "NSI responds with HTML Gateway Time-out" in
        forAll(
          genPaymentIdentifierRequests
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>
            val htmlError = "<html><body><h1>504 Gateway Time-out</h1>The server didn't respond in time.</body></html>"

            stubNsiMakePaymentHtml(504, htmlError)

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

      "NSI responds with another error" in
        forAll(
          genPaymentIdentifierRequests
        ) { request =>
          withCaptureOfLoggingFrom(LOGGER) { logs =>

            stubNsiMakePaymentError()

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
