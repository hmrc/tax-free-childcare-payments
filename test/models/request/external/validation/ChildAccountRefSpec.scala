/*
 * Copyright 2026 HM Revenue & Customs
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

package models.request.external.validation

import helpers.BaseSpec
import helpers.generators.request.external.validation.ChildAccountRefGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

class ChildAccountRefSpec extends BaseSpec with ChildAccountRefGenerators {

  "reads" should {
    "return JsSuccess" when {
      Seq(
        "AA0012345TFC",
        "ZZ..98765TFC",
        "XY  99999TFC",
        "PO--54321TFC",
        "AB''00000TFC"
      ).foreach { childAccountRefString =>
        s"child account ref is known good value '$childAccountRefString'" in {
          JsString(childAccountRefString).validate[ChildAccountRef] shouldBe JsSuccess(
            ChildAccountRef(childAccountRefString)
          )
        }
      }
      "child account ref is valid value from generator" in
        forAll(genChildAccountRefStrings) { childAccountRefString =>
          JsString(childAccountRefString).validate[ChildAccountRef] shouldBe JsSuccess(
            ChildAccountRef(childAccountRefString)
          )
        }
    }

    "return JsError" when {

      Seq(
        ""             -> "child account ref is empty string",
        "AA0012345"    -> "child account ref does not end with 'TFC'",
        "120012345TFC" -> "child account ref does not start with two letters",
        "AA1112345TFC" -> "child account ref does not not have two special characters in middle"
      ).foreach { case (childAccountRefString, message) =>
        message in {
          JsString(childAccountRefString).validate[ChildAccountRef] shouldBe JsError("error.pattern")
        }
      }

      "child account ref is invalid value from generator" in
        forAll(genInvalidChildAccountRefStrings) {
          checkJsonValidationError[ChildAccountRef](
            expectedJsonPath = "",
            expectedMessage = "error.pattern"
          )
        }

    }

  }

}
