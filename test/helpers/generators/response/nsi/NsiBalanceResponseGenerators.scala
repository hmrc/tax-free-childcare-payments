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

package helpers.generators.response.nsi

import helpers.generators.request.external.validation.PositiveIntGenerators
import models.response.nsi.NsiBalanceResponse
import org.scalacheck.Gen

trait NsiBalanceResponseGenerators extends NsiAccountStatusGenerators with PositiveIntGenerators {

  protected val genNsiBalanceResponses: Gen[NsiBalanceResponse] = for {
    accountStatus  <- genNsiAccountStatuses
    topUpAvailable <- genPositiveInts
    topUpRemaining <- genPositiveInts
    paidIn         <- genPositiveInts
    totalBalance   <- genPositiveInts
    clearedFunds   <- genPositiveInts
  } yield NsiBalanceResponse(
    accountStatus = accountStatus,
    topUpAvailable = topUpAvailable,
    topUpRemaining = topUpRemaining,
    paidIn = paidIn,
    totalBalance = totalBalance,
    clearedFunds = clearedFunds
  )

}
