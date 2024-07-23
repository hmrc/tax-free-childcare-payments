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

import models.requests.LinkRequest

class LinkRequestSpec extends BaseSpec {
  "API JSON reader" should {

    "return JsError" when {
      "TFC account ref is missing" in
        forAll(linkPayloadsWithMissingTfcAccountRef) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "outbound_child_payment_ref",
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(linkPayloadsWithInvalidTfcAccountRef) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "outbound_child_payment_ref",
            expectedMessage = "error.pattern"
          )
        }

      "EPP URN is missing" in
        forAll(linkPayloadsWithMissingEppUrn) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "epp_reg_reference",
            expectedMessage = "error.path.missing"
          )
        }

      "EPP URN is invalid" in
        forAll(linkPayloadsWithInvalidEppUrn) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "epp_reg_reference",
            expectedMessage = "error.pattern"
          )
        }

      "EPP account ID is missing" in
        forAll(linkPayloadsWithMissingEppAccountId) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "epp_unique_customer_id",
            expectedMessage = "error.path.missing"
          )
        }

      "EPP account ID is invalid" in
        forAll(linkPayloadsWithInvalidEppAccountId) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "epp_unique_customer_id",
            expectedMessage = "error.pattern"
          )
        }

      "child DoB is missing" in
        forAll(linkPayloadsWithMissingChildDob) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "child_date_of_birth",
            expectedMessage = "error.path.missing"
          )
        }

      "child DoB is invalid" in
        forAll(linkPayloadsWithInvalidChildDob) {
          checkJsonError[LinkRequest](
            expectedJsonPath = "child_date_of_birth",
            expectedMessage = "error.expected.date.isoformat"
          )
        }
    }
  }
}
