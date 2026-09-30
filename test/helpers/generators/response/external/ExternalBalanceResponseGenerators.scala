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

package helpers.generators.response.external

import helpers.generators.request.external.validation.PositiveIntGenerators
import models.response.external.ExternalBalanceResponse
import org.scalacheck.Gen

trait ExternalBalanceResponseGenerators extends PositiveIntGenerators with ExternalAccountStatusGenerators {

  protected given genExternalBalanceResponses: Gen[ExternalBalanceResponse] = for {
    tfc_account_status <- genExternalAccountStatuses
    government_top_up  <- genPositiveInts
    top_up_allowance   <- genPositiveInts
    paid_in_by_you     <- genPositiveInts
    total_balance      <- genPositiveInts
    cleared_funds      <- genPositiveInts
  } yield ExternalBalanceResponse(
    tfc_account_status = tfc_account_status,
    government_top_up = government_top_up,
    top_up_allowance = top_up_allowance,
    paid_in_by_you = paid_in_by_you,
    total_balance = total_balance,
    cleared_funds = cleared_funds
  )

}
