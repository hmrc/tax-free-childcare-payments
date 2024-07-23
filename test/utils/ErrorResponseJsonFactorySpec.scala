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
import models.requests.{LinkRequest, PaymentRequest, SharedRequestData}
import org.scalatest.EitherValues

class ErrorResponseJsonFactorySpec extends BaseSpec with models.request.Generators with EitherValues {
  "method getJson" should {
    "return expected errorCode and errorDescription" when {
      "LinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[LinkRequest].asEither.left.value

              val errorResponseJson = ErrorResponseJsonFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }

      "SharedRequestData JSON is invalid" in
        forAll(balanceRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[SharedRequestData].asEither.left.value

              val errorResponseJson = ErrorResponseJsonFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }

      "PaymentRequest JSON is invalid" in
        forAll(paymentRequestJsonErrorScenarios) {
          (_, invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[PaymentRequest].asEither.left.value

              val errorResponseJson = ErrorResponseJsonFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }
    }
  }

  private lazy val linkRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code"),
    (linkPayloadsWithInvalidTfcAccountRef, "E0000"),
    (linkPayloadsWithInvalidEppUrn, "E0000"),
    (linkPayloadsWithInvalidEppAccountId, "E0000"),
    (linkPayloadsWithMissingTfcAccountRef, "E0001"),
    (linkPayloadsWithMissingEppUrn, "E0002"),
    (linkPayloadsWithMissingEppAccountId, "E0004"),
    (linkPayloadsWithMissingChildDob, "E0006"),
    (linkPayloadsWithInvalidChildDob, "E0021")
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
    ("Description","Invalid Payloads", "Expected Error Code"),
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
    ("String payment amount", paymentPayloadsWithStringPaymentAmount, "E0023")
  )
}
