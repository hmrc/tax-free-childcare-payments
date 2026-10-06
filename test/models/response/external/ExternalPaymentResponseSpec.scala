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

package models.response.external

import helpers.BaseSpec
import play.api.libs.json.{JsString, Json}

import java.time.LocalDate

class ExternalPaymentResponseSpec extends BaseSpec {

  private val externalPaymentResponse = ExternalPaymentResponse(
    payment_reference = "payment_reference",
    estimated_payment_date = LocalDate.of(2026, 9, 30)
  )

  private val externalPaymentResponseJson = Json.obj(
    "payment_reference"      -> JsString("payment_reference"),
    "estimated_payment_date" -> JsString("2026-09-30")
  )

  "writes" should {
    "return the correct JSON" in {
      Json.toJson(externalPaymentResponse) shouldBe externalPaymentResponseJson
    }
  }

}
