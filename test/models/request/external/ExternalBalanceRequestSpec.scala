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
import helpers.generators.request.external.ExternalBalanceRequestGenerators
import play.api.libs.json.JsSuccess

class ExternalBalanceRequestSpec extends BaseSpec with ExternalBalanceRequestGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"

  "reads" should {

    "return JsSuccess" when {
      "model is valid" in
        forAll(genExternalBalanceRequestJsObjects) { jsObject =>
          jsObject.validate[ExternalBalanceRequest] shouldBe a[JsSuccess[?]]
        }
    }

    "return JsError" when {
      "outbound_child_payment_ref is missing" in
        forAll(genExternalBalanceRequestJsObjectsWithoutOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(genExternalBalanceRequestJsObjectsWithInvalidOutboundChildPaymentRef) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = outbound_child_payment_ref,
            expectedMessage = "error.pattern"
          )
        }

      "epp_reg_reference is missing" in
        forAll(genExternalBalanceRequestJsObjectsWithoutEppRegReference) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_reg_reference is invalid" in
        forAll(genExternalBalanceRequestJsObjectsWithInvalidEppRegReference) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = epp_reg_reference,
            expectedMessage = "error.pattern"
          )
        }

      "epp_unique_customer_id is missing" in
        forAll(genExternalBalanceRequestJsObjectsWithoutEppUniqueCustomerId) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.path.missing"
          )
        }

      "epp_unique_customer_id is invalid" in
        forAll(genExternalBalanceRequestJsObjectsWithInvalidEppUniqueCustomerId) {
          checkJsonValidationError[ExternalBalanceRequest](
            expectedJsonPath = epp_unique_customer_id,
            expectedMessage = "error.pattern"
          )
        }

    }
  }

}
