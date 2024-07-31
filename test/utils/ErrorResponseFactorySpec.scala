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

package utils

import base.BaseSpec
import ch.qos.logback.classic.Level
import models.requests.{LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse._
import org.apache.pekko.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, LoneElement}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsDefined, JsString, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class ErrorResponseFactorySpec extends BaseSpec
    with models.request.Generators
    with EitherValues
    with LogCapturing
    with Status
    with ScalaFutures {
  private implicit val as: ActorSystem   = ActorSystem(getClass.getSimpleName)
  private implicit val rh: RequestHeader = FakeRequest()

  private val expectedLogger            = Logger(classOf[ErrorResponseFactory.type])

  "method getJson" should {
    "return expected errorCode and errorDescription" when {
      "LinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) {
          (_, invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[LinkRequest].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }

      "SharedRequestData JSON is invalid" in
        forAll(balanceRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[SharedRequestData].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }

      "PaymentRequest JSON is invalid" in
        forAll(paymentRequestJsonErrorScenarios) {
          (_, invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[PaymentRequest].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }
    }
  }

  "method getResult" should {
    "return expected status, errorCode, errorDescription and log expected message" in
      forAll(nsiErrorScenarios) { (nsiError, expectedStatus, expectedCode, expectedLogLvl, expectedLogMsg) =>
        withCaptureOfLoggingFrom(expectedLogger) { logs =>
          val response     = ErrorResponseFactory getResult nsiError
          val responseBody = response.body.consumeData.futureValue.toArray
          val responseJson = Json parse responseBody

          response.header.status shouldBe expectedStatus
          responseJson \ "errorCode" shouldBe JsDefined(JsString(expectedCode))

          checkLog(
            expectedLogLvl,
            expectedMessage = s"$expectedCode - $expectedLogMsg"
          )(logs)
        }
      }
  }

  private lazy val linkRequestJsonErrorScenarios = Table(
    ("Description", "Invalid Payloads", "Expected Error Code"),
    ("Invalid TFC account ref", linkPayloadsWithInvalidTfcAccountRef, "E0000"),
    ("Invalid EPP URN", linkPayloadsWithInvalidEppUrn, "E0000"),
    ("Invalid EPP Account ID", linkPayloadsWithInvalidEppAccountId, "E0000"),
    ("Missing TFC account ref", linkPayloadsWithMissingTfcAccountRef, "E0001"),
    ("Missing EPP URN", linkPayloadsWithMissingEppUrn, "E0002"),
    ("Missing EPP account ID", linkPayloadsWithMissingEppAccountId, "E0004"),
    ("Missing child DOB", linkPayloadsWithMissingChildDob, "E0006"),
    ("Non-string child DOB", linkPayloadsWithNonStringChildDob, "E0021"),
    ("Non-ISO-8061 child DOB", linkPayloadsWithNonIso8061ChildDob, "E0021")
  )

  private lazy val balanceRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code"),
    (sharedPayloadsWithInvalidTfcAccountRef, "E0000"),
    (sharedPayloadsWithInvalidEppUrn, "E0000"),
    (sharedPayloadsWithInvalidEppAccountId, "E0000"),
    (sharedPayloadsWithMissingTfcAccountRef, "E0001"),
    (sharedPayloadsWithMissingEppUrn, "E0002"),
    (sharedPayloadsWithMissingEppAccountId, "E0004")
  )

  private lazy val paymentRequestJsonErrorScenarios = Table(
    ("Description", "Invalid Payloads", "Expected Error Code"),
    ("Invalid TFC account ref", paymentPayloadsWithInvalidTfcAccountRef, "E0000"),
    ("Invalid EPP URN", paymentPayloadsWithInvalidEppUrn, "E0000"),
    ("Invalid EPP account ID", paymentPayloadsWithInvalidEppAccountId, "E0000"),
    ("Missing TFC account Ref", paymentPayloadsWithMissingTfcAccountRef, "E0001"),
    ("Missing EPP URN", paymentPayloadsWithMissingEppUrn, "E0002"),
    ("Missing EPP account ID", paymentPayloadsWithMissingEppAccountId, "E0004"),
    ("Missing Payee Type", paymentPayloadsWithMissingPayeeType, "E0007"),
    ("Invalid Payee Type", paymentPayloadsWithInvalidPayeeType, "E0022"),
    ("Missing CCP URN", paymentPayloadsWithMissingCcpUrn, "E0003"),
    ("Invalid CCP URN", paymentPayloadsWithInvalidCcpUrn, "E0000"),
    ("Missing CCP postcode", paymentPayloadsWithMissingCcpPostcode, "E0000"),
    ("Invalid CCP postcode", paymentPayloadsWithInvalidCcpPostcode, "E0000"),
    ("Missing payment amount", paymentPayloadsWithMissingPaymentAmount, "E0008"),
    ("Fractional payment amount", paymentPayloadsWithFractionalPaymentAmount, "E0023"),
    ("String payment amount", paymentPayloadsWithStringPaymentAmount, "E0023"),
    ("Non-positive payment amount", paymentPayloadsWithNonPositivePaymentAmount, "E0023")
  )

  private lazy val nsiErrorScenarios = Table(
    ("NSI Response", "Expected Status", "Expected Error Code", "Expected Log Level", "Expected Log Message"),
    (E0000, INTERNAL_SERVER_ERROR, "E0000", Level.WARN, "Invalid input data"),
    (E0001, INTERNAL_SERVER_ERROR, "E0001", Level.WARN, "childAccountPaymentRef is missing"),
    (E0002, INTERNAL_SERVER_ERROR, "E0002", Level.WARN, "eppURN is missing"),
    (E0003, INTERNAL_SERVER_ERROR, "E0003", Level.WARN, "ccpURN is missing"),
    (E0004, INTERNAL_SERVER_ERROR, "E0004", Level.WARN, "eppAccount is missing"),
    (E0005, INTERNAL_SERVER_ERROR, "E0005", Level.WARN, "parentNino is missing"),
    (E0006, INTERNAL_SERVER_ERROR, "E0006", Level.WARN, "childDob is missing"),
    (E0007, INTERNAL_SERVER_ERROR, "E0007", Level.WARN, "payeeType is missing"),
    (E0008, INTERNAL_SERVER_ERROR, "E0008", Level.WARN, "amount is missing"),
    (E0020, BAD_GATEWAY, "E0020", Level.WARN, "parentNino does not match expected format (AANNNNNNA)"),
    (E0021, INTERNAL_SERVER_ERROR, "E0021", Level.WARN, "childDob does not match expected format (YYYY-MM-DD)"),
    (E0022, INTERNAL_SERVER_ERROR, "E0022", Level.WARN, "payeeType value should be one of ['CCP','EPP']"),
    (E0023, INTERNAL_SERVER_ERROR, "E0023", Level.WARN, "amount most be a number"),
    (E0024, BAD_REQUEST, "E0024", Level.INFO, "eppAccount does not correlate with the provided eppURN"),
    (E0025, BAD_REQUEST, "E0025", Level.INFO, "childDob does not correlate with the provided childAccountPaymentRef"),
    (E0026, BAD_REQUEST, "E0026", Level.INFO, "childAccountPaymentRef is not related to parentNino"),
    (E0401, INTERNAL_SERVER_ERROR, "E0401", Level.WARN, "Authentication information is missing or invalid"),
    (E0030, BAD_REQUEST, "E0030", Level.INFO, "EPP is not Active"),
    (E0031, BAD_REQUEST, "E0031", Level.INFO, "CCP is not Active"),
    (E0032, BAD_REQUEST, "E0032", Level.INFO, "EPP is not linked to Child Account"),
    (E0033, BAD_REQUEST, "E0033", Level.INFO, "Insufficient funds"),
    (E0034, SERVICE_UNAVAILABLE, "E0034", Level.WARN, "Error returned from banking services"),
    (E0035, BAD_REQUEST, "E0035", Level.INFO, "Payments from this TFC account are blocked"),
    (E0040, BAD_REQUEST, "E0040", Level.INFO, "childAccountPaymentRef not found"),
    (E0041, BAD_REQUEST, "E0041", Level.INFO, "eppURN not found"),
    (E0042, BAD_REQUEST, "E0042", Level.INFO, "ccpURN not found"),
    (E0043, BAD_REQUEST, "E0043", Level.INFO, "parentNino not found"),
    (E9000, SERVICE_UNAVAILABLE, "E9000", Level.WARN, "Internal server error"),
    (E9999, SERVICE_UNAVAILABLE, "E9999", Level.WARN, "Error during execution"),
    (E8000, SERVICE_UNAVAILABLE, "E8000", Level.WARN, "Service not available"),
    (E8001, SERVICE_UNAVAILABLE, "E8001", Level.WARN, "Service not available due to lack of connection to provider"),
    (ETFC3, BAD_GATEWAY, "ETFC3", Level.WARN, "Unexpected NSI response")
  )
}
