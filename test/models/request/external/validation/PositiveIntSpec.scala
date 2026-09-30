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
import helpers.generators.request.external.validation.PositiveIntGenerators
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess, JsValue}

class PositiveIntSpec extends BaseSpec with PositiveIntGenerators {

  "reads" should {

    "return JsSuccess" when {

      Seq(
        1,
        599,
        329847
      ).foreach { positiveInt =>
        s"positive int is known good value '$positiveInt'" in {
          JsNumber(positiveInt).validate[PositiveInt] shouldBe JsSuccess(PositiveInt(positiveInt))
        }

      }

      "positive int is known good value from generator" in
        forAll(genPositiveInts) { positiveInt =>
          JsNumber(positiveInt).validate[PositiveInt] shouldBe JsSuccess(PositiveInt(positiveInt))
        }

    }

    "return JsError" when
      Seq[(JsValue, String)](
        JsNumber(0)   -> "positive int is zero",
        JsNumber(-1)  -> "positive int is negative",
        JsNumber(1.5) -> "positive int is fractional",
        JsString("1") -> "positive int is string representation instead of number"
      ).foreach { case (positiveInt, message) =>
        message in {
          positiveInt.validate[PositiveInt] shouldBe a[JsError]
        }
      }

  }

}
