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
import helpers.generators.request.external.validation.PayeeTypeGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

class PayeeTypeSpec extends BaseSpec with PayeeTypeGenerators {

  "reads" should {

    "return JsSuccess" when {

      "payee type is known good value 'CPP'" in {
        JsString("CCP").validate[PayeeType] shouldBe JsSuccess(PayeeType.CCP)
      }

      "payee type is valid value from generator" in
        forAll(genPayeeTypeStrings)(payeeTypeString =>
          JsString(payeeTypeString).validate[PayeeType] shouldBe a[JsSuccess[?]]
        )

    }

    "return JsError" when {

      Seq(
        ""    -> "payee type is empty string",
        "EPP" -> "payee type is 'EPP'"
      ).foreach { case (payeeTypeString, message) =>
        message in {
          JsString(payeeTypeString).validate[PayeeType] shouldBe JsError("error.payee_type")
        }
      }

      "payee type is invalid value from generator" in
        forAll(genInvalidPayeeTypeStrings) {
          checkJsonValidationError[PayeeType](
            expectedJsonPath = "",
            expectedMessage = "error.payee_type"
          )
        }

    }

  }

}
