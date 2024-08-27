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
import models.request.data.SharedRequestGenerators

class SharedRequestDataSpec extends BaseSpec with SharedRequestGenerators {

  "API JSON reader" should {
    "return JsSuccess" when {
      "given valid input JSON" in
        forAll(validSharedDataModels) { expectedSharedDataModel =>
          val validJson = getJsonFrom(expectedSharedDataModel)

          val actualSharedDataModel = validJson.validate[SharedRequestData].asEither.value

          actualSharedDataModel shouldBe expectedSharedDataModel
        }
    }

    "return JsError" when {
      "field outbound_child_payment_ref is missing" in
        forAll(sharedPayloadsWithMissingTfcAccountRef) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "outbound_child_payment_ref",
            expectedMessage = "error.path.missing"
          )
        }

      "field outbound_child_payment_ref is invalid" in
        forAll(sharedPayloadsWithInvalidTfcAccountRef) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "outbound_child_payment_ref",
            expectedMessage = "error.pattern"
          )
        }

      "field epp_reg_reference is missing" in
        forAll(sharedPayloadsWithMissingEppUrn) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "epp_reg_reference",
            expectedMessage = "error.path.missing"
          )
        }

      "field epp_reg_reference is invalid" in
        forAll(sharedPayloadsWithInvalidEppUrn) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "epp_reg_reference",
            expectedMessage = "error.pattern"
          )
        }

      "field epp_unique_customer_id is missing" in
        forAll(sharedPayloadsWithMissingEppAccountId) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "epp_unique_customer_id",
            expectedMessage = "error.path.missing"
          )
        }

      "field epp_unique_customer_id is invalid" in
        forAll(sharedPayloadsWithInvalidEppAccountId) {
          checkJsonError[SharedRequestData](
            expectedJsonPath = "epp_unique_customer_id",
            expectedMessage = "error.pattern"
          )
        }
    }
  }
}
