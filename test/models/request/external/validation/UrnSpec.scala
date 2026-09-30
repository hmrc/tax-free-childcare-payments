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
import helpers.generators.request.external.validation.UrnGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

class UrnSpec extends BaseSpec with UrnGenerators {

  "reads" should {
    "return JsSuccess" when {
      Seq(
        "12345",
        "1",
        "12345678901234567890",
        "abcdefghijklmnopqrst",
        "acdefghijk1234567890"
      ).foreach { urnString =>
        s"urn is known good value'$urnString'" in {
          JsString(urnString).validate[Urn] shouldBe JsSuccess(Urn(urnString))
        }
      }

      "value is valid value from generator" in
        forAll(genUrnStrings)(urnString => JsString(urnString).validate[Urn] shouldBe JsSuccess(Urn(urnString)))
    }

    "return JsError" when
      Seq(
        ""                       -> "urn is empty string",
        "0123456789012345678901" -> "urn contains more than 20 characters"
      ).foreach { case (urnString, message) =>
        message in {
          JsString(urnString).validate[Urn] shouldBe JsError("error.pattern")
        }
      }

    "urn is invalid value form generator" in
      forAll(genInvalidUrnStrings) {
        checkJsonValidationError[Urn](
          expectedJsonPath = "",
          expectedMessage = "error.pattern"
        )
      }

  }

}
