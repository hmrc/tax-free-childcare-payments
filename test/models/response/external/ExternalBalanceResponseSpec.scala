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
import play.api.libs.json.{JsNumber, JsString, Json}

class ExternalBalanceResponseSpec extends BaseSpec {

  private val externalBalanceResponse =
    ExternalBalanceResponse(
      tfc_account_status = ExternalAccountStatus.ACTIVE,
      government_top_up = 1,
      top_up_allowance = 2,
      paid_in_by_you = 3,
      total_balance = 4,
      cleared_funds = 5
    )

  private val externalBalanceResponseJson = Json.obj(
    "tfc_account_status" -> JsString("ACTIVE"),
    "government_top_up"  -> JsNumber(1),
    "top_up_allowance"   -> JsNumber(2),
    "paid_in_by_you"     -> JsNumber(3),
    "total_balance"      -> JsNumber(4),
    "cleared_funds"      -> JsNumber(5)
  )

  "writes" should {
    "format to the correct JSON object" in {
      Json.toJson(externalBalanceResponse) shouldBe externalBalanceResponseJson
    }
  }

}
