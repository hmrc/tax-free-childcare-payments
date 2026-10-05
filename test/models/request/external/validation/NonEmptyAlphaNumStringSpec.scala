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
import helpers.generators.request.external.validation.NonEmptyAlphaNumStringGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

class NonEmptyAlphaNumStringSpec extends BaseSpec with NonEmptyAlphaNumStringGenerators {

  "reads" should {

    "return JsSuccess" when {

      Seq(
        "1",
        "abc",
        "abcde12345"
      ).foreach { nonEmptyAlphaNumString =>
        s"non empty alpha num string is known good value '$nonEmptyAlphaNumString'" in {
          JsString(nonEmptyAlphaNumString).validate[NonEmptyAlphaNumString] shouldBe JsSuccess(
            NonEmptyAlphaNumString(nonEmptyAlphaNumString)
          )
        }

      }

      "non empty alpha num string is valid value from generator" in
        forAll(genNonEmptyAlphaNumStrings) { nonEmptyAlphaNumString =>
          JsString(nonEmptyAlphaNumString).validate[NonEmptyAlphaNumString] shouldBe JsSuccess(
            NonEmptyAlphaNumString(nonEmptyAlphaNumString)
          )
        }

    }

    "return JsError" when {

      Seq(
        ""              -> "non empty alpha num string is empty string",
        "."             -> "non empty alpha num string contains non-alphanumeric character",
        "1".repeat(256) -> "non empty alpha num string is too long",
        " 1"            -> "non empty alpha num string contains leading whitespace",
        "1 "            -> "non empty alpha num string contains trailing whitespace",
        "1 1"           -> "non empty alpha num string contains interior whitespace"
      ).foreach { case (nonEmptyAlphaNumString, message) =>
        message in {
          JsString(nonEmptyAlphaNumString).validate[NonEmptyAlphaNumString] shouldBe JsError("error.pattern")
        }
      }

      "non empty alpha num string is invalid value from generator" in
        forAll(genInvalidNonEmptyAlphaNumStrings) {
          checkJsonValidationError[NonEmptyAlphaNumString](
            expectedJsonPath = "",
            expectedMessage = "error.pattern"
          )
        }

    }

  }

}
