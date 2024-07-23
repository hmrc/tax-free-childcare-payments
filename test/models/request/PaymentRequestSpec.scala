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

package models.request

import models.requests.PaymentRequest
import models.requests.PaymentRequest.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY, PAYMENT_AMOUNT_KEY}
import models.requests.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}

class PaymentRequestSpec extends BaseSpec {

  "API JSON reader" should {

    "return JsError" when {
      "field outbound_child_payment_ref is missing" in
        forAll(paymentPayloadsWithMissingTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "field outbound_child_payment_ref is invalid" in
        forAll(paymentPayloadsWithInvalidTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "field epp_reg_reference is missing" in
        forAll(paymentPayloadsWithMissingEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "field epp_reg_reference is invalid" in
        forAll(paymentPayloadsWithInvalidEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "field epp_unique_customer_id is missing" in
        forAll(paymentPayloadsWithMissingEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "field epp_unique_customer_id is invalid" in
        forAll(paymentPayloadsWithInvalidEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payee type is missing" in
        forAll(paymentPayloadsWithMissingPayeeType) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is invalid" in
        forAll(paymentPayloadsWithInvalidPayeeType) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "CCP URN is missing" in
        forAll(paymentPayloadsWithMissingCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "CCP URN is invalid" in
        forAll(paymentPayloadsWithInvalidCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "CCP postcode is missing" in
        forAll(paymentPayloadsWithMissingCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "CCP postcode is invalid" in
        forAll(paymentPayloadsWithInvalidCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payment amount is missing" in
        forAll(paymentPayloadsWithMissingPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payment amount is fractional" in
        forAll(paymentPayloadsWithFractionalPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.int"
          )
        }

      "payment amount is not numeric" in
        forAll(paymentPayloadsWithStringPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.jsnumber"
          )
        }
    }
  }
}
