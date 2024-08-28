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
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.scenarios._
import models.request.data.Generators
import models.request.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.EitherValues
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest

class NsiConnectorISpec
  extends BaseISpec
    with NsiStubs
    with EitherValues
    with Generators
    with models.response.Generators {
  private val connector = app.injector.instanceOf[NsiConnector]

  "method linkAccounts" should {
    "return Right LinkResponse" when {
      "NSI responds 201 with expected JSON format" in
        forAll { scenario: NsiLinkAccounts201Scenario =>
          stubNsiLinkAccounts201(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[LinkRequest] = scenario.identifierRequest

          val actualResponse = connector.linkAccounts.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(getRequestedFor(nsiLinkAccountsUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left E0001 and log errorDescription" when {
      "NSI responds with error status, errorCode E0001, and defined errorDescription" in
        forAll(
          arbitrary[IdentifierRequest[LinkRequest]],
          Gen.asciiPrintableStr
        ) {
          (request, expectedErrorDescription) =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>
              val expectedStatus = randomHttpErrorCodes.sample.get
              stubNsiLinkAccountsError(expectedStatus, "E0001", expectedErrorDescription)

              val actualNsiErrorResponse = connector.linkAccounts(request).futureValue.left.value

              actualNsiErrorResponse shouldBe E0001

              val expectedResponseJson = Json.obj("errorCode" -> "E0001", "errorDescription" -> expectedErrorDescription)
              val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0001"
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
        ) {
          (request, expectedErrorDescription) =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>
              val expectedStatus = randomHttpErrorCodes.sample.get
              stubNsiLinkAccountsError(expectedStatus, "E0024", expectedErrorDescription)

              val actualNsiErrorResponse = connector.linkAccounts(request).futureValue.left.value

              actualNsiErrorResponse shouldBe E0024

              val expectedResponseJson = Json.obj("errorCode" -> "E0024", "errorDescription" -> expectedErrorDescription)
              val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0024"
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
          arbitrary[IdentifierRequest[LinkRequest]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiLinkAccountsError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.linkAccounts(request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }

  "method checkBalance" should {
    "return Right(BalanceResponse)" when {
      s"NSI responds $OK with expected JSON format" in
        forAll { scenario: NsiCheckBalance200Scenario =>
          stubNsiCheckBalance200(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[SharedRequestData] = scenario.identifierRequest

          val actualResponse = connector.checkBalance.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(getRequestedFor(nsiBalanceUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left ETFC3" when {
      implicit val shr: Shrink[String] = Shrink.shrinkAny

      "NSI responds with an invalid account status" in
        forAll(randomNinos, Gen.uuid, validSharedDataModels) { (nino, correlationId, sharedRequestData) =>
          implicit val req: IdentifierRequest[SharedRequestData] =
            IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), sharedRequestData))

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
              .willReturn(created() withBody invalidBalanceResponse.toString)
          }

          val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll(
          arbitrary[IdentifierRequest[SharedRequestData]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiCheckBalanceError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.checkBalance(request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }

  "method makePayment" should {
    "return Right PaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in
        forAll { scenario: NsiMakePayment201Scenario =>
          stubNsiMakePayment201(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[PaymentRequest] = scenario.identifierRequest

          val actualResponse = connector.makePayment.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(postRequestedFor(nsiPaymentUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left E0009 and log errorDescription" when {
      "NSI responds with error status, errorCode E0027, and defined errorDescription" in
        forAll(
          arbitrary[IdentifierRequest[PaymentRequest]],
          Gen.asciiPrintableStr
        ) {
          (request, expectedErrorDescription) =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>
              val expectedStatus = randomHttpErrorCodes.sample.get
              stubNsiMakePaymentError(expectedStatus, "E0009", expectedErrorDescription)

              val actualNsiErrorResponse = connector.makePayment(request).futureValue.left.value

              actualNsiErrorResponse shouldBe E0009

              val expectedResponseJson = Json.obj("errorCode" -> "E0009", "errorDescription" -> expectedErrorDescription)
              val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0009"
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
        ) {
          (request, expectedErrorDescription) =>
            withCaptureOfLoggingFrom(LOGGER) { logs =>
              val expectedStatus = randomHttpErrorCodes.sample.get
              stubNsiMakePaymentError(expectedStatus, "E0027", expectedErrorDescription)

              val actualNsiErrorResponse = connector.makePayment(request).futureValue.left.value

              actualNsiErrorResponse shouldBe E0027

              val expectedResponseJson = Json.obj("errorCode" -> "E0027", "errorDescription" -> expectedErrorDescription)
              val expectedPartialLogMessage = s"NSI responded $expectedStatus with body $expectedResponseJson - triggering E0027"
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
          arbitrary[IdentifierRequest[PaymentRequest]],
          randomUnknownErrorCodes
        ) { (request, unknownErrorCode) =>
          stubNsiMakePaymentError(BAD_REQUEST, unknownErrorCode, "An error occurred")

          val actualNsiErrorResponse = connector.makePayment(request).futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }

  private def getFullLogMessageFrom(partialLogMessage: String) = s"[Error] - [ ] - [null: $partialLogMessage]"

  private lazy val LOGGER = Logger(classOf[NsiConnector.type])
}
