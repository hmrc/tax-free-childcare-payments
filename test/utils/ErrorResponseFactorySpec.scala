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
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
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
  private implicit val as: ActorSystem = ActorSystem(getClass.getSimpleName)
  private implicit val rh: RequestHeader = FakeRequest()

  private val expectedLogger = Logger(classOf[ErrorResponseFactory.type])

  "method getJson" should {
    "return expected errorCode and errorDescription" when {
      "LinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[LinkRequest].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
            }
        }

      "SharedRequestData JSON is invalid" in
        forAll(balanceRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[SharedRequestData].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
            }
        }

      "PaymentRequest JSON is invalid" in
        forAll(paymentRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[PaymentRequest].asEither.left.value

              val errorResponseJson = ErrorResponseFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
            }
        }
    }
  }

  "method getResult" should {
    "return expected status, errorCode, errorDescription and log expected message" in
      forAll(nsiErrorScenarios) { (nsiError, expectedStatus, expectedCode, expectedLogLvl, expectedLogContent) =>
        withCaptureOfLoggingFrom(expectedLogger) { logs =>
          val response = ErrorResponseFactory getResult nsiError
          val responseBody = response.body.consumeData.futureValue.toArray
          val responseJson = Json parse responseBody

          response.header.status shouldBe expectedStatus
          responseJson \ "errorCode" shouldBe JsDefined(JsString(expectedCode))

          val expectedLogMessage = s"[Error] - [payment] - [null: $expectedLogContent]"
          checkLog(expectedLogLvl, expectedLogMessage)(logs)
        }
      }
  }

  private lazy val linkRequestJsonErrorScenarios = Table(
    ("Invalid Payloads",                   "Expected Error Code", "Expected Error Description"),
    (linkPayloadsWithInvalidTfcAccountRef, "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (linkPayloadsWithMissingTfcAccountRef, "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (linkPayloadsWithMissingEppUrn,        "E0002",               "epp_reg_reference is in invalid format or missing"),
    (linkPayloadsWithInvalidEppUrn,        "E0002",               "epp_reg_reference is in invalid format or missing"),
    (linkPayloadsWithMissingEppAccountId,  "E0004",               "epp_unique_customer_id is in invalid format or missing"),
    (linkPayloadsWithInvalidEppAccountId,  "E0004",               "epp_unique_customer_id is in invalid format or missing"),
    (linkPayloadsWithMissingChildDob,      "E0006",               "child_date_of_birth is in invalid format or missing"),
    (linkPayloadsWithNonStringChildDob,    "E0006",               "child_date_of_birth is in invalid format or missing"),
    (linkPayloadsWithNonIso8061ChildDob,   "E0006",               "child_date_of_birth is in invalid format or missing")
  )

  private lazy val balanceRequestJsonErrorScenarios = Table(
    ("Invalid Payloads",                     "Expected Error Code", "Expected Error Description"),
    (sharedPayloadsWithInvalidTfcAccountRef, "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (sharedPayloadsWithMissingTfcAccountRef, "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (sharedPayloadsWithMissingEppUrn,        "E0002",               "epp_reg_reference is in invalid format or missing"),
    (sharedPayloadsWithInvalidEppUrn,        "E0002",               "epp_reg_reference is in invalid format or missing"),
    (sharedPayloadsWithMissingEppAccountId,  "E0004",               "epp_unique_customer_id is in invalid format or missing"),
    (sharedPayloadsWithInvalidEppAccountId,  "E0004",               "epp_unique_customer_id is in invalid format or missing")
  )

  private lazy val paymentRequestJsonErrorScenarios = Table(
    ("Invalid Payloads",                          "Expected Error Code", "Expected Error Description"),
    (paymentPayloadsWithInvalidTfcAccountRef,     "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (paymentPayloadsWithMissingTfcAccountRef,     "E0001",               "outbound_child_payment_ref is in invalid format or missing"),
    (paymentPayloadsWithMissingEppUrn,            "E0002",               "epp_reg_reference is in invalid format or missing"),
    (paymentPayloadsWithInvalidEppUrn,            "E0002",               "epp_reg_reference is in invalid format or missing"),
    (paymentPayloadsWithMissingEppAccountId,      "E0004",               "epp_unique_customer_id is in invalid format or missing"),
    (paymentPayloadsWithInvalidEppAccountId,      "E0004",               "epp_unique_customer_id is in invalid format or missing"),
    (paymentPayloadsWithMissingPayeeType,         "E0007",               "payee_type is in invalid format or missing"),
    (paymentPayloadsWithInvalidPayeeType,         "E0007",               "payee_type is in invalid format or missing"),
    (paymentPayloadsWithMissingCcpUrn,            "E0003",               "ccp_reg_reference is in invalid format or missing"),
    (paymentPayloadsWithInvalidCcpUrn,            "E0003",               "ccp_reg_reference is in invalid format or missing"),
    (paymentPayloadsWithMissingCcpPostcode,       "E0009",               "ccp_postcode is in invalid format or missing"),
    (paymentPayloadsWithInvalidCcpPostcode,       "E0009",               "ccp_postcode is in invalid format or missing"),
    (paymentPayloadsWithMissingPaymentAmount,     "E0008",               "payment_amount is in invalid format or missing"),
    (paymentPayloadsWithFractionalPaymentAmount,  "E0008",               "payment_amount is in invalid format or missing"),
    (paymentPayloadsWithStringPaymentAmount,      "E0008",               "payment_amount is in invalid format or missing"),
    (paymentPayloadsWithNonPositivePaymentAmount, "E0008",               "payment_amount is in invalid format or missing")
  )

  private lazy val nsiErrorScenarios = Table(
    ("Error", "Expected Status",     "Code",  "Log Level", "Expected Log Content"),
    (E0000,   INTERNAL_SERVER_ERROR, "E0000", Level.WARN,  "E0000 - Invalid input data"),
    (E0001,   INTERNAL_SERVER_ERROR, "E0001", Level.WARN,  "E0001 - childAccountPaymentRef is missing"),
    (E0002,   INTERNAL_SERVER_ERROR, "E0002", Level.WARN,  "E0002 - eppURN is missing"),
    (E0003,   INTERNAL_SERVER_ERROR, "E0003", Level.WARN,  "E0003 - ccpURN is missing"),
    (E0004,   INTERNAL_SERVER_ERROR, "E0004", Level.WARN,  "E0004 - eppAccount is missing"),
    (E0005,   INTERNAL_SERVER_ERROR, "E0005", Level.WARN,  "E0005 - parentNino is missing"),
    (E0006,   INTERNAL_SERVER_ERROR, "E0006", Level.WARN,  "E0006 - childDob is missing"),
    (E0007,   INTERNAL_SERVER_ERROR, "E0007", Level.WARN,  "E0007 - payeeType is missing"),
    (E0008,   INTERNAL_SERVER_ERROR, "E0008", Level.WARN,  "E0008 - amount is missing"),
    (E0020,   BAD_GATEWAY,           "E0020", Level.WARN,  "E0020 - parentNino does not match expected format (AANNNNNNA)"),
    (E0021,   INTERNAL_SERVER_ERROR, "E0021", Level.WARN,  "E0021 - childDob does not match expected format (YYYY-MM-DD)"),
    (E0022,   INTERNAL_SERVER_ERROR, "E0022", Level.WARN,  "E0022 - payeeType value should be one of ['CCP','EPP']"),
    (E0023,   INTERNAL_SERVER_ERROR, "E0023", Level.WARN,  "E0023 - amount most be a number"),
    (E0024,   BAD_REQUEST,           "E0024", Level.INFO,  "E0024 - eppAccount does not correlate with the provided eppURN"),
    (E0025,   BAD_REQUEST,           "E0025", Level.INFO,  "E0025 - childDob does not correlate with the provided childAccountPaymentRef"),
    (E0026,   BAD_REQUEST,           "E0026", Level.INFO,  "E0026 - childAccountPaymentRef is not related to parentNino"),
    (E0401,   INTERNAL_SERVER_ERROR, "E0401", Level.WARN,  "E0401 - Authentication information is missing or invalid"),
    (E0030,   BAD_REQUEST,           "E0030", Level.INFO,  "E0030 - EPP is not Active"),
    (E0031,   BAD_REQUEST,           "E0031", Level.INFO,  "E0031 - CCP is not Active"),
    (E0032,   BAD_REQUEST,           "E0032", Level.INFO,  "E0032 - EPP is not linked to Child Account"),
    (E0033,   BAD_REQUEST,           "E0033", Level.INFO,  "E0033 - Insufficient funds"),
    (E0034,   SERVICE_UNAVAILABLE,   "E0034", Level.WARN,  "E0034 - Error returned from banking services"),
    (E0035,   BAD_REQUEST,           "E0035", Level.INFO,  "E0035 - Payments from this TFC account are blocked"),
    (E0040,   BAD_REQUEST,           "E0040", Level.INFO,  "E0040 - childAccountPaymentRef not found"),
    (E0041,   BAD_REQUEST,           "E0041", Level.INFO,  "E0041 - eppURN not found"),
    (E0042,   BAD_REQUEST,           "E0042", Level.INFO,  "E0042 - ccpURN not found"),
    (E0043,   BAD_REQUEST,           "E0043", Level.INFO,  "E0043 - parentNino not found"),
    (E9000,   SERVICE_UNAVAILABLE,   "E9000", Level.WARN,  "E9000 - Internal server error"),
    (E9999,   SERVICE_UNAVAILABLE,   "E9999", Level.WARN,  "E9999 - Error during execution"),
    (E8000,   SERVICE_UNAVAILABLE,   "E8000", Level.WARN,  "E8000 - Service not available"),
    (E8001,   SERVICE_UNAVAILABLE,   "E8001", Level.WARN,  "E8001 - Service not available due to lack of connection to provider"),
    (ETFC3,   BAD_GATEWAY,           "ETFC3", Level.WARN,  "ETFC3 - Unexpected NSI response")
  )
}
