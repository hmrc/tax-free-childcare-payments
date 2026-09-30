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
import helpers.generators.request.external.ExternalLinkRequestGenerators
import play.api.libs.json.{JsError, JsPath, JsSuccess}

class ExternalLinkRequestSpec extends BaseSpec with ExternalLinkRequestGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"
  private val child_date_of_birth        = "child_date_of_birth"

  "reads" should {

    "return JsSuccess" when {
      "model is valid" in
        forAll(genExternalLinkRequestJsObjects) { jsObject =>
          jsObject.validate[ExternalLinkRequest] shouldBe a[JsSuccess[?]]
        }
    }

    "return JsError" when {
      "outbound_child_payment_ref is missing" in
        forAll(genExternalLinkRequestJsObjectsWithoutOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(genExternalLinkRequestJsObjectsWithInvalidOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.pattern"
          )
        }

      "epp_reg_reference is missing" in
        forAll(genExternalLinkRequestJsObjectsWithoutEppRegReference) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_reg_reference is invalid" in
        forAll(genExternalLinkRequestJsObjectsWithInvalidEppRegReference) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.pattern"
          )
        }

      "epp_unique_customer_id is missing" in
        forAll(genExternalLinkRequestJsObjectsWithoutEppUniqueCustomerId) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_unique_customer_id is invalid" in
        forAll(genExternalLinkRequestJsObjectsWithInvalidEppUniqueCustomerId) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.pattern"
          )
        }

      "child_date_of_birth is missing" in
        forAll(genExternalLinkRequestJsObjectsWithoutChildDateOfBirth) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = child_date_of_birth,
            expectedMessage = "error.path.missing"
          )
        }

      "child_date_of_birth is not a string" in
        forAll(genExternalLinkRequestJsObjectsWithNonStringChildDateOfBirth) {
          checkJsonValidationError[ExternalLinkRequest](
            expectedJsonPath = child_date_of_birth,
            expectedMessage = "error.expected.jsstring"
          )
        }

      "child_date_of_birth is a string not conforming to ISO 8061" in
        forAll(genExternalLinkRequestJsObjectsWithNonIso8601ChildDateOfBirth) { jsObject =>
          val jsError = jsObject.validate[ExternalLinkRequest].asInstanceOf[JsError]

          jsError.errors.head._1 shouldBe (JsPath \ child_date_of_birth)
        }
    }
  }

}
