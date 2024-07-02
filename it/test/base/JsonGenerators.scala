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

package base

import models.requests.PaymentRequest.PayeeType
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

trait JsonGenerators extends Generators {
  import org.scalacheck.Gen

  protected val validLinkAccountsRequestPayloads: Gen[JsObject] =
    for {
      eppAuthPayload <- validSharedPayloads
      childAgeDays   <- Gen.chooseNum(0, 18 * 365)
    } yield eppAuthPayload ++ Json.obj(
      "child_date_of_birth" -> (LocalDate.now() minusDays childAgeDays)
    )

  protected val validCheckBalanceRequestPayloads: Gen[JsObject] = validSharedPayloads

  protected val validLMakePaymentRequestPayloads: Gen[JsObject] =
    for {
      eppAuthPayload     <- validSharedPayloads
      optCCP             <- Gen option childCareProviders
      paymentAmountPence <- Gen.posNum[Int]
    } yield eppAuthPayload ++ Json.obj(
      "payee_type"        -> (if (optCCP.isDefined) PayeeType.CCP else PayeeType.EPP),
      "ccp_reg_reference" -> optCCP.map(_.urn),
      "ccp_postcode"      -> optCCP.map(_.postcode),
      "payment_amount"    -> paymentAmountPence
    )

  private lazy val validSharedPayloads =
    for {
      account_ref <- nonEmptyAlphaNumStrings
      epp_urn     <- nonEmptyAlphaNumStrings
      epp_account <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> account_ref,
      "epp_reg_reference" -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )
}