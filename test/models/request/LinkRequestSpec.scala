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
import models.requests.LinkRequest.CHILD_DOB_KEY
import models.requests.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}

import play.api.libs.json.{JsPath, JsonValidationError, KeyPathNode}

class LinkRequestSpec extends BaseSpec {
  "API JSON reader" should {

    "return JsError" when {
      "TFC account ref is missing" in
        forAll(linkPayloadsWithMissingTfcAccountRef) {
          checkJsonError[LinkRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "TFC account ref is invalid" in
        forAll(linkPayloadsWithInvalidTfcAccountRef) {
          checkJsonError[LinkRequest](
            expectedJsonPath = TFC_ACCOUNT_REF_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP URN is missing" in
        forAll(linkPayloadsWithMissingEppUrn) {
          checkJsonError[LinkRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP URN is invalid" in
        forAll(linkPayloadsWithInvalidEppUrn) {
          checkJsonError[LinkRequest](
            expectedJsonPath = EPP_URN_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "EPP account ID is missing" in
        forAll(linkPayloadsWithMissingEppAccountId) {
          checkJsonError[LinkRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "EPP account ID is invalid" in
        forAll(linkPayloadsWithInvalidEppAccountId) {
          checkJsonError[LinkRequest](
            expectedJsonPath = EPP_ACCOUNT_ID_KEY,
            expectedMessage = "error.pattern"
          )
        }

      "child DoB is missing" in
        forAll(linkPayloadsWithMissingChildDob) {
          checkJsonError[LinkRequest](
            expectedJsonPath = CHILD_DOB_KEY,
            expectedMessage = "error.path.missing"
          )
        }

      "child DoB is not a string" in
        forAll(linkPayloadsWithNonStringChildDob) {
          checkJsonError[LinkRequest](
            expectedJsonPath = CHILD_DOB_KEY,
            expectedMessage = "error.expected.jsstring"
          )
        }

      "child DoB is a string not conforming to ISO 8061" in
        forAll(linkPayloadsWithNonIso8061ChildDob) {
          checkJsonError[LinkRequest] { (jsonPath: JsPath, _: JsonValidationError) =>
            jsonPath.path.loneElement shouldBe KeyPathNode(CHILD_DOB_KEY)
          }
        }
    }
  }
}
