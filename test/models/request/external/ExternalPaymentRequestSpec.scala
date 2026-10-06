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

package models.request.external

import helpers.BaseSpec
import helpers.generators.request.external.ExternalPaymentRequestGenerators
import models.request.external.validation.{
  ChildAccountRef,
  NonEmptyAlphaNumString,
  PayeeType,
  PositiveInt,
  PostCode,
  Urn
}
import models.request.nsi.NsiPaymentRequest

class ExternalPaymentRequestSpec extends BaseSpec with ExternalPaymentRequestGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"
  private val payment_amount             = "payment_amount"
  private val payee_type                 = "payee_type"
  private val ccp_reg_reference          = "ccp_reg_reference"
  private val ccp_postcode               = "ccp_postcode"

  "reads" should {
    "return JsError" when {
      "outbound_child_payment_ref is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.pattern"
          )
        }

      "epp_reg_reference is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutEppRegReference) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_reg_reference is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidEppRegReference) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.pattern"
          )
        }

      "epp_unique_customer_id is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutEppUniqueCustomerId) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_unique_customer_id is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidEppUniqueCustomerId) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.pattern"
          )
        }
      "payee_type is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutPayeeType) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payee_type,
            expectedMessage = "error.path.missing"
          )
        }

      "payee_type is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidPayeeType) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payee_type,
            expectedMessage = "error.payee_type"
          )
        }

      "ccp_reg_reference" in
        forAll(genExternalPaymentRequestJsObjectsWithoutCcpRegReference) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = ccp_reg_reference,
            expectedMessage = "error.path.missing"
          )
        }

      "ccp_reg_reference is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidCcpRegReference) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = ccp_reg_reference,
            expectedMessage = "error.pattern"
          )
        }

      "ccp_postcode is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutCcpPostcode) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = ccp_postcode,
            expectedMessage = "error.path.missing"
          )
        }

      "ccp_postcode is invalid" in
        forAll(genExternalPaymentRequestJsObjectsWithInvalidCcpPostcode) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = ccp_postcode,
            expectedMessage = "error.pattern"
          )
        }

      "payment_amount is missing" in
        forAll(genExternalPaymentRequestJsObjectsWithoutPaymentAmount) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payment_amount,
            expectedMessage = "error.path.missing"
          )
        }

      "payment_amount is fractional" in
        forAll(genExternalPaymentRequestJsObjectsWithFractionalPaymentAmount) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payment_amount,
            expectedMessage = "error.expected.int"
          )
        }

      "payment_amount is not numeric" in
        forAll(genExternalPaymentRequestJsObjectsWithStringPaymentAmount) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payment_amount,
            expectedMessage = "error.expected.jsnumber"
          )
        }

      "payment amount is not positive" in
        forAll(genExternalPaymentRequestJsObjectsWithNegativePaymentAmount) {
          checkJsonValidationError[ExternalPaymentRequest](
            expectedJsonPath = payment_amount,
            expectedMessage = "error.min",
            messageArgs = 1
          )
        }
    }
  }

  "toNsiPaymentRequest" should {
    "return the correct model" in {
      val external = ExternalPaymentRequest(
        outbound_child_payment_ref = ChildAccountRef("outbound_child_payment_ref"),
        epp_reg_reference = NonEmptyAlphaNumString("epp_reg_reference"),
        epp_unique_customer_id = NonEmptyAlphaNumString("epp_unique_customer_id"),
        payment_amount = PositiveInt(1000),
        payee_type = PayeeType.CCP,
        ccp_reg_reference = Urn("ccp_reg_reference"),
        ccp_postcode = PostCode("ccp_postcode")
      )

      external.toNsiPaymentRequest shouldBe NsiPaymentRequest(
        eppAccount = "epp_unique_customer_id",
        eppURN = "epp_reg_reference",
        amount = 1000,
        childAccountPaymentRef = "outbound_child_payment_ref",
        payeeType = PayeeType.CCP,
        ccpURN = "ccp_reg_reference",
        ccpPostcode = "ccp_postcode"
      )
    }
  }

}
