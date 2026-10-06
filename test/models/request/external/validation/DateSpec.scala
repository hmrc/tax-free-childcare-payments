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
import helpers.generators.request.external.validation.DateGenerators
import play.api.libs.json.{JsError, JsString, JsSuccess}

import java.time.LocalDate

class DateSpec extends BaseSpec with DateGenerators {

  "reads" should {
    "return JsSuccess" when {
      Seq(
        "2026-09-24" -> LocalDate.of(2026, 9, 24),
        "2026-04-30" -> LocalDate.of(2026, 4, 30)
      ).foreach { (dateString, parsedDate) =>
        s"date is known good value '$dateString'" in {
          JsString(dateString).validate[Date] shouldBe JsSuccess(Date(parsedDate))
        }
      }

      "date is valid value from generator" in
        forAll(genIso8601DateStrings)(dateString => JsString(dateString).validate[Date] shouldBe a[JsSuccess[?]])

    }
    "return JsError" when {
      Seq(
        ""           -> "date is empty string",
        "2026/09/24" -> "date is in YYYY/MM/DD (not iso8601)",
        "24/09/2026" -> "date is in DD/MM/YYYY (not iso8601)",
        "2026-09-31" -> "date is in valid format, but is not a real date",
        "2026-9-24"  -> "date is missing leading zero on one component"
      ).foreach { case (dateString, message) =>
        message in {
          JsString(dateString).validate[Date] shouldBe a[JsError]
        }
      }

      "date is invalid value from generator" in
        forAll(genInvalidIso8601DateStrings)(dateString => JsString(dateString).validate[Date] shouldBe a[JsError])
    }
  }

}
