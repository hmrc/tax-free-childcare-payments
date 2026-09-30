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

package models.response.nsi

import helpers.BaseSpec
import models.response.external.{ExternalAccountStatus, ExternalBalanceResponse}
import play.api.libs.json.{JsNumber, JsSuccess, Json}

class NsiBalanceResponseSpec extends BaseSpec {

  private val nsiBalanceResponseJson = Json.obj(
    "accountStatus"  -> "ACTIVE",
    "topUpAvailable" -> JsNumber(1),
    "topUpRemaining" -> JsNumber(2),
    "paidIn"         -> JsNumber(3),
    "totalBalance"   -> JsNumber(4),
    "clearedFunds"   -> JsNumber(5)
  )

  private val nsiBalanceResponse =
    NsiBalanceResponse(
      accountStatus = NsiAccountStatus.ACTIVE,
      topUpAvailable = 1,
      topUpRemaining = 2,
      paidIn = 3,
      totalBalance = 4,
      clearedFunds = 5
    )

  private val externalBalanceResponse =
    ExternalBalanceResponse(
      tfc_account_status = ExternalAccountStatus.ACTIVE,
      government_top_up = 1,
      top_up_allowance = 2,
      paid_in_by_you = 3,
      total_balance = 4,
      cleared_funds = 5
    )

  "reads" should {
    "correctly parse a valid JSON response" in {
      nsiBalanceResponseJson.validate[NsiBalanceResponse] shouldBe JsSuccess(nsiBalanceResponse)
    }
  }

  "toExternalBalanceResponse" should {
    "return the correct data" in {
      nsiBalanceResponse.toExternalBalanceResponse shouldBe externalBalanceResponse
    }
  }

}
