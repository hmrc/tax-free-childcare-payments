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
import helpers.generators.request.external.validation.PostCodeGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

class PostCodeSpec extends BaseSpec with PostCodeGenerators {

  "reads" should {
    "return JsSuccess" when {
      Seq(
        "SW1A 2AA",
        "SW1A2AA",
        " SW1A    2AA",
        " SW1A2AA",
        " SW1A   2AA   ",
        "   SW1A2AA ",
        "SW1A 2AA  ",
        "SW1A2AA         "
      ).foreach { postCodeString =>
        s"post code is valid value '$postCodeString'" in {
          JsString(postCodeString).validate[PostCode] shouldBe JsSuccess(PostCode(postCodeString))
        }

      }

      "post code is valid value from generator" in
        forAll(genPostCodeStrings) { postCodeString =>
          JsString(postCodeString).validate[PostCode] shouldBe JsSuccess(PostCode(postCodeString))
        }
    }

    "return JsError" when {
      Seq(
        ""          -> "post code is empty string",
        "S 2AA"     -> "post code doesn't contain enough items in first group",
        "SW1AA 2AA" -> "post code contains too many items in first group",
        "SW1A 2A"   -> "post code doesn't contain enough items in second group",
        "SW1A 2AAA" -> "post code contains too many items in second group",
        "SW1A AA"   -> "second group doesn't start with a digit",
        "SW1A 22AA" -> "second group starts with multiple digits"
      ).foreach { case (postCodeString, message) =>
        message in {
          JsString(postCodeString).validate[PostCode] shouldBe JsError("error.pattern")
        }

      }

      "post code is invalid value from generator" in
        forAll(genInvalidPostCodeStrings) {
          checkJsonValidationError[PostCode](
            expectedJsonPath = "",
            expectedMessage = "error.pattern"
          )
        }
    }
  }

}
