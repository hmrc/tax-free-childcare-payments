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

package models.request

import java.time.LocalDate

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

trait Generators extends base.Generators {
  import org.scalacheck.Gen

  /** START Link Accounts generators */
  protected val validLinkAccountsRequestPayloads: Gen[JsObject] =
    for {
      eppAuthPayload <- validSharedPayloads
      childAgeDays   <- Gen.chooseNum(0, 18 * 365)
    } yield eppAuthPayload ++ Json.obj(
      "child_date_of_birth" -> (LocalDate.now() minusDays childAgeDays)
    )

  protected lazy val randomLinkRequestJsonWithMissingChildDob: Gen[JsValue] = validSharedPayloads

  protected lazy val randomLinkRequestJsonWithInvalidChildDob: Gen[JsObject] = for {
    sharedPayload     <- validSharedPayloads
    invalidDateString <- Gen.alphaNumStr
  } yield sharedPayload + ("child_date_of_birth" -> JsString(invalidDateString))

  /** END Link Accounts generators
    *
    * BEGIN Check Balance generators
    */
  protected val validCheckBalanceRequestPayloads: Gen[JsObject] = validSharedPayloads

  /** END Check Balance generators
    *
    * BEGIN Make Payment generators
    */
  protected val validPaymentRequestWithPayeeTypeSetToCCP: Gen[JsObject] =
    for {
      eppAuthPayload     <- validSharedPayloads
      ccp                <- childCareProviders
      paymentAmountPence <- Gen.posNum[Int]
    } yield eppAuthPayload ++ Json.obj(
      "payee_type"        -> "CCP",
      "ccp_reg_reference" -> ccp.urn,
      "ccp_postcode"      -> ccp.postcode,
      "payment_amount"    -> paymentAmountPence
    )

  protected val validPaymentRequestWithPayeeTypeSetToccp: Gen[JsObject] =
    for {
      eppAuthPayload     <- validSharedPayloads
      ccp                <- childCareProviders
      paymentAmountPence <- Gen.posNum[Int]
    } yield eppAuthPayload ++ Json.obj(
      "payee_type"        -> "ccp",
      "ccp_reg_reference" -> ccp.urn,
      "ccp_postcode"      -> ccp.postcode,
      "payment_amount"    -> paymentAmountPence
    )

  protected val validEppPaymentRequestWithPayeeTypeSetToEPP: Gen[JsObject] =
    for {
      eppAuthPayload     <- validSharedPayloads
      paymentAmountPence <- Gen.posNum[Int]
    } yield eppAuthPayload ++ Json.obj(
      "payee_type"     -> "EEP",
      "payment_amount" -> paymentAmountPence
    )

  /** END Make Payment generators
    *
    * BEGIN Shared generators
    */

  protected lazy val validSharedPayloads: Gen[JsObject] =
    for {
      epp_urn     <- nonEmptyAlphaNumStrings
      epp_account <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> "AbCd12345TFC",
      "epp_reg_reference"          -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )
}
