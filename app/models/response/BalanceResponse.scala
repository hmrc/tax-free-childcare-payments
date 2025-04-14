/*
 * Copyright 2024 HM Revenue & Customs
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

package models.response

import play.api.libs.json.{Json, Writes}

/** All currency quantities are in pence. For example, £250.00 is given as 25000. */
final case class BalanceResponse(
    accountStatus: NsiAccountStatus,
    topUpAvailable: Int,
    topUpRemaining: Int,
    paidIn: Int,
    totalBalance: Int,
    clearedFunds: Int
)

object BalanceResponse {

  implicit val writesToAPI: Writes[BalanceResponse] = br =>
    Json.obj(
      "tfc_account_status" -> br.accountStatus,
      "government_top_up"  -> br.topUpAvailable,
      "top_up_allowance"   -> br.topUpRemaining,
      "paid_in_by_you"     -> br.paidIn,
      "total_balance"      -> br.totalBalance,
      "cleared_funds"      -> br.clearedFunds
    )

}
