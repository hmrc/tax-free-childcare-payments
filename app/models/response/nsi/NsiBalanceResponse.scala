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

import models.response.external.ExternalBalanceResponse
import play.api.libs.json.{Json, Reads}

final case class NsiBalanceResponse(
    accountStatus: NsiAccountStatus,
    topUpAvailable: Int,
    topUpRemaining: Int,
    paidIn: Int,
    totalBalance: Int,
    clearedFunds: Int
) {

  def toExternalBalanceResponse: ExternalBalanceResponse = ExternalBalanceResponse(
    tfc_account_status = accountStatus.toExternalAccountStatus,
    government_top_up = topUpAvailable,
    top_up_allowance = topUpRemaining,
    paid_in_by_you = paidIn,
    total_balance = totalBalance,
    cleared_funds = clearedFunds
  )

}

object NsiBalanceResponse {
  given Reads[NsiBalanceResponse] = Json.reads[NsiBalanceResponse]
}
