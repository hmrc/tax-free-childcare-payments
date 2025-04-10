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
import models.request.data.Generators
import models.request.{LinkRequest, Payee, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse._
import org.apache.pekko.actor.ActorSystem
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures

import play.api.http.Status
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class ErrorResponseFactorySpec
    extends BaseSpec
    with Generators
    with EitherValues
    with LogCapturing
    with Status
    with ScalaFutures {
  private implicit val as: ActorSystem = ActorSystem(getClass.getSimpleName)

  "method getJson" should {
    "return expected errorCode and errorDescription" when {
      implicit val readsPayee: Reads[Payee] = Payee.readsCcpFromApi

      "LinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
          forAll(invalidPayloads) { payload =>
            val jsErrors = payload.validate[LinkRequest].asEither.left.value

            val errorResponseJson = ErrorResponseFactory.getJson(jsErrors)

            checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
          }
        }

      "SharedRequestData JSON is invalid" in
        forAll(balanceRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
          forAll(invalidPayloads) { payload =>
            val jsErrors = payload.validate[SharedRequestData].asEither.left.value

            val errorResponseJson = ErrorResponseFactory.getJson(jsErrors)

            checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
          }
        }

      "PaymentRequest JSON is invalid" in
        forAll(paymentRequestJsonErrorScenarios) { (invalidPayloads, expectedErrorCode, expectedErrorDesc) =>
          forAll(invalidPayloads) { payload =>
            val jsErrors = payload.validate[PaymentRequest].asEither.left.value

            val errorResponseJson = ErrorResponseFactory.getJson(jsErrors)

            checkErrorJson(errorResponseJson, expectedErrorCode, expectedErrorDesc)
          }
        }
    }
  }

  "method getResult" should {
    "return expected status, errorCode, errorDescription and log expected message" in
      forAll(nsiErrorScenarios) { (nsiError, expectedStatus, expectedErrorCode, expectedErrorDescription) =>
        val response     = ErrorResponseFactory.getResult(nsiError)
        val responseBody = response.body.consumeData.futureValue.toArray
        val responseJson = Json.parse(responseBody)

        response.header.status shouldBe expectedStatus
        checkErrorJson(
          responseJson,
          expectedErrorCode,
          expectedErrorDescription
        )
      }
  }

  private lazy val linkRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (linkPayloadsWithMissingTfcAccountRef, "E0001", "outbound_child_payment_ref is in invalid format or missing"),
    (linkPayloadsWithInvalidTfcAccountRef, "E0001", "outbound_child_payment_ref is in invalid format or missing"),
    (linkPayloadsWithMissingEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (linkPayloadsWithInvalidEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (linkPayloadsWithMissingEppAccountId, "E0004", "epp_unique_customer_id is in invalid format or missing"),
    (linkPayloadsWithInvalidEppAccountId, "E0004", "epp_unique_customer_id is in invalid format or missing"),
    (linkPayloadsWithMissingChildDob, "E0006", "child_date_of_birth is in invalid format or missing"),
    (linkPayloadsWithNonStringChildDob, "E0006", "child_date_of_birth is in invalid format or missing"),
    (linkPayloadsWithNonIso8061ChildDob, "E0006", "child_date_of_birth is in invalid format or missing")
  )

  private lazy val balanceRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (sharedPayloadsWithInvalidTfcAccountRef, "E0001", "outbound_child_payment_ref is in invalid format or missing"),
    (sharedPayloadsWithMissingTfcAccountRef, "E0001", "outbound_child_payment_ref is in invalid format or missing"),
    (sharedPayloadsWithMissingEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (sharedPayloadsWithInvalidEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (sharedPayloadsWithMissingEppAccountId, "E0004", "epp_unique_customer_id is in invalid format or missing"),
    (sharedPayloadsWithInvalidEppAccountId, "E0004", "epp_unique_customer_id is in invalid format or missing")
  )

  private lazy val paymentRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code", "Expected Error Description"),
    (
      randomPaymentJsonWithCcpOnlyAndInvalidTfcAccountRef,
      "E0001",
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (
      randomPaymentJsonWithCcpOnlyAndMissingTfcAccountRef,
      "E0001",
      "outbound_child_payment_ref is in invalid format or missing"
    ),
    (randomPaymentJsonWithCcpOnlyAndMissingEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndInvalidEppUrn, "E0002", "epp_reg_reference is in invalid format or missing"),
    (
      randomPaymentJsonWithCcpOnlyAndMissingEppAccountId,
      "E0004",
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (
      randomPaymentJsonWithCcpOnlyAndInvalidEppAccountId,
      "E0004",
      "epp_unique_customer_id is in invalid format or missing"
    ),
    (randomPaymentJsonWithMissingPayeeType, "E0007", "payee_type is in invalid format or missing"),
    (randomPaymentJsonWithPayeeTypeNotCCP, "E0007", "payee_type is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndMissingCcpUrn, "E0003", "ccp_reg_reference is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndInvalidCcpUrn, "E0003", "ccp_reg_reference is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndMissinCcpPostcode, "E0009", "ccp_postcode is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndInvalidCcpPostcode, "E0009", "ccp_postcode is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndMissingPaymentAmount, "E0008", "payment_amount is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndFractionalPaymentAmount, "E0008", "payment_amount is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndStringPaymentAmount, "E0008", "payment_amount is in invalid format or missing"),
    (randomPaymentJsonWithCcpOnlyAndNonPositivePaymentAmount, "E0008", "payment_amount is in invalid format or missing")
  )

  private lazy val nsiErrorScenarios = Table(
    ("Error", "Expected Status", "Expected Error Code", "Expected Error Description"),
    (E0000, INTERNAL_SERVER_ERROR, "E0000", EXPECTED_500_DESC),
    (E0001, INTERNAL_SERVER_ERROR, "E0001", EXPECTED_500_DESC),
    (E0002, INTERNAL_SERVER_ERROR, "E0002", EXPECTED_500_DESC),
    (E0003, INTERNAL_SERVER_ERROR, "E0003", EXPECTED_500_DESC),
    (E0004, INTERNAL_SERVER_ERROR, "E0004", EXPECTED_500_DESC),
    (E0005, INTERNAL_SERVER_ERROR, "E0005", EXPECTED_500_DESC),
    (E0006, INTERNAL_SERVER_ERROR, "E0006", EXPECTED_500_DESC),
    (E0007, INTERNAL_SERVER_ERROR, "E0007", EXPECTED_500_DESC),
    (E0008, INTERNAL_SERVER_ERROR, "E0008", EXPECTED_500_DESC),
    (E0020, BAD_GATEWAY, "E0020", EXPECTED_502_DESC),
    (E0021, INTERNAL_SERVER_ERROR, "E0021", EXPECTED_500_DESC),
    (E0022, INTERNAL_SERVER_ERROR, "E0022", EXPECTED_500_DESC),
    (E0023, INTERNAL_SERVER_ERROR, "E0023", EXPECTED_500_DESC),
    (E0024, BAD_REQUEST, "E0024", EXPECTED_E0024_DESC),
    (E0025, BAD_REQUEST, "E0025", EXPECTED_E0025_DESC),
    (E0026, BAD_REQUEST, "E0026", EXPECTED_E0026_DESC),
    (E0027, BAD_REQUEST, "E0027", EXPECTED_E0027_DESC),
    (E0401, INTERNAL_SERVER_ERROR, "E0401", EXPECTED_500_DESC),
    (E0030, BAD_REQUEST, "E0030", EXPECTED_E0030_DESC),
    (E0031, BAD_REQUEST, "E0031", EXPECTED_E0031_DESC),
    (E0032, BAD_REQUEST, "E0032", EXPECTED_E0032_DESC),
    (E0033, BAD_REQUEST, "E0033", EXPECTED_E0033_DESC),
    (E0034, SERVICE_UNAVAILABLE, "E0034", EXPECTED_503_DESC),
    (E0035, BAD_REQUEST, "E0035", EXPECTED_E0035_DESC),
    (E0036, BAD_REQUEST, "E0036", EXPECTED_E0036_DESC),
    (E0042, BAD_REQUEST, "E0042", EXPECTED_E0042_DESC),
    (E0043, BAD_REQUEST, "E0043", EXPECTED_E0043_DESC),
    (E9000, SERVICE_UNAVAILABLE, "E9000", EXPECTED_503_DESC),
    (E9999, SERVICE_UNAVAILABLE, "E9999", EXPECTED_503_DESC),
    (E8000, SERVICE_UNAVAILABLE, "E8000", EXPECTED_503_DESC),
    (E8001, SERVICE_UNAVAILABLE, "E8001", EXPECTED_503_DESC),
    (ETFC3, BAD_GATEWAY, "ETFC3", EXPECTED_502_DESC),
    (ETFC4, BAD_GATEWAY, "ETFC4", EXPECTED_502_DESC)
  )

}
