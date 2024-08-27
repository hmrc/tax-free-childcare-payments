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

import base.BaseSpec
import models.request.Payee.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY}
import models.request.PaymentRequest.PAYMENT_AMOUNT_KEY
import models.request.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}
import models.request.data.PaymentRequestGenerators
import play.api.libs.json.Reads

class PaymentRequestSpec extends BaseSpec with PaymentRequestGenerators {

  "With implicit Payee Reads in scope, API Reads" should {
    implicit val readsPayee: Reads[Payee] = Payee.readsPayeeFromApi

    "return JsError" when {
      "TFC account ref is missing" in
        forAll(randomPaymentJsonWithAnyPayeeAndMissingTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(randomPaymentJsonWithAnyPayeeAndInvalidTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP URN is missing" in
        forAll(randomPaymentJsonWithAnyPayeeAndMissingEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP URN is invalid" in
        forAll(randomPaymentJsonWithAnyPayeeAndInvalidEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP account ID is missing" in
        forAll(randomPaymentJsonWithAnyPayeeAndMissingEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP account ID is invalid" in
        forAll(randomPaymentJsonWithAnyPayeeAndInvalidEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payee type is missing" in
        forAll(randomPaymentJsonWithMissingPayeeType) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is invalid" in
        forAll(randomPaymentJsonWithInvalidPayeeType) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.payee_type"
          )
        }

      "payee type is CCP & CCP URN is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is CCP & CCP URN is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payee type is CCP & CCP postcode is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissinCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is CCP & CCP postcode is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payment amount is missing" in
        forAll(randomPaymentJsonWithAnyPayeeAndMissingPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payment amount is fractional" in
        forAll(randomPaymentJsonWithAnyPayeeAndFractionalPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.int"
          )
        }

      "payment amount is not numeric" in
        forAll(randomPaymentJsonWithAnyPayeeAndStringPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.jsnumber"
          )
        }

      "payment amount is not positive" in
        forAll(randomPaymentJsonWithAnyPayeeAndNonPositivePaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.min"
          )
        }
    }
  }

  "With implicit CCP Reads in scope, API Reads" should {
    implicit val readsCcp: Reads[Payee] = Payee.readsCcpFromApi

    "return JsError" when {
      "TFC account ref is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidTfcAccountRef) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP URN is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP URN is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidEppUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP account ID is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP account ID is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidEppAccountId) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payee type is missing" in
        forAll(randomPaymentJsonWithMissingPayeeType) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is not CCP" in
        forAll(randomPaymentJsonWithPayeeTypeNotCCP) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYEE_TYPE_KEY,
            expectedMessage = "error.payee_type"
          )
        }

      "payee type is CCP & CCP URN is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is CCP & CCP URN is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidCcpUrn) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payee type is CCP & CCP postcode is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissinCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payee type is CCP & CCP postcode is invalid" in
        forAll(randomPaymentJsonWithCcpOnlyAndInvalidCcpPostcode) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = CCP_POSTCODE_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "payment amount is missing" in
        forAll(randomPaymentJsonWithCcpOnlyAndMissingPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "payment amount is fractional" in
        forAll(randomPaymentJsonWithCcpOnlyAndFractionalPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.int"
          )
        }

      "payment amount is not numeric" in
        forAll(randomPaymentJsonWithCcpOnlyAndStringPaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.expected.jsnumber"
          )
        }

      "payment amount is not positive" in
        forAll(randomPaymentJsonWithCcpOnlyAndNonPositivePaymentAmount) {
          checkJsonError[PaymentRequest](
            expectedJsonPath = PAYMENT_AMOUNT_KEY,
            expectedMessage = "error.min"
          )
        }
    }
  }
}
