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

import models.requests.SharedRequestData

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

trait Generators extends base.Generators {
  import org.scalacheck.Gen

  /** START Link Accounts generators */
  protected val validLinkPayloads: Gen[JsObject] = linkPayloadsWith(validSharedPayloads)

  protected lazy val linkPayloadsWithMissingTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingTfcAccountRef)

  protected lazy val linkPayloadsWithInvalidTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidTfcAccountRef)

  protected lazy val linkPayloadsWithMissingChildDob: Gen[JsValue] = validSharedPayloads

  protected lazy val linkPayloadsWithInvalidChildDob: Gen[JsObject] = for {
    sharedPayload     <- validSharedPayloads
    invalidDateString <- Gen.alphaNumStr
  } yield sharedPayload + ("child_date_of_birth" -> JsString(invalidDateString))

  private def linkPayloadsWith(sharedPayloads: Gen[JsObject]) =
    for {
      sharedPayload <- sharedPayloads
      childAgeDays  <- Gen.chooseNum(0, 18 * 365)
    } yield sharedPayload ++ Json.obj(
      "child_date_of_birth" -> (LocalDate.now() minusDays childAgeDays)
    )

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

  protected lazy val validSharedDataModels: Gen[SharedRequestData] =
    for {
      epp_account     <- nonEmptyAlphaNumStrings
      epp_urn         <- nonEmptyAlphaNumStrings
      childAccountRef <- validChildAccountRefs
    } yield SharedRequestData(
      epp_unique_customer_id = epp_account,
      epp_reg_reference = epp_urn,
      outbound_child_payment_ref = childAccountRef
    )

  protected def getJsonFrom(model: SharedRequestData): JsObject = Json.obj(
    "outbound_child_payment_ref" -> model.outbound_child_payment_ref,
    "epp_reg_reference"          -> model.epp_reg_reference,
    "epp_unique_customer_id"     -> model.epp_unique_customer_id
  )

  protected lazy val sharedPayloadsWithMissingTfcAccountRef: Gen[JsObject] =
    for {
      epp_urn     <- nonEmptyAlphaNumStrings
      epp_account <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "epp_reg_reference"      -> epp_urn,
      "epp_unique_customer_id" -> epp_account
    )

  protected lazy val sharedPayloadsWithInvalidTfcAccountRef: Gen[JsObject] =
    for {
      childAccountRef <- invalidChildAccountRefs
      epp_urn         <- nonEmptyAlphaNumStrings
      epp_account     <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> childAccountRef,
      "epp_reg_reference"          -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )

  protected lazy val sharedPayloadsWithMissingEppUrn: Gen[JsObject] =
    for {
      childAccountRef <- validChildAccountRefs
      epp_account     <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> childAccountRef,
      "epp_unique_customer_id"     -> epp_account
    )

  protected lazy val sharedPayloadsWithInvalidEppUrn: Gen[JsObject] =
    for {
      childAccountRef <- validChildAccountRefs
      epp_urn         <- nonAlphaNumStrings
      epp_account     <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> childAccountRef,
      "epp_reg_reference"          -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )

  protected lazy val sharedPayloadsWithMissingEppAccountId: Gen[JsObject] =
    for {
      childAccountRef <- validChildAccountRefs
      epp_urn         <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> childAccountRef,
      "epp_reg_reference"          -> epp_urn
    )

  protected lazy val sharedPayloadsWithInvalidEppAccountId: Gen[JsObject] =
    for {
      childAccountRef <- validChildAccountRefs
      epp_urn         <- nonEmptyAlphaNumStrings
      epp_account     <- nonAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> childAccountRef,
      "epp_reg_reference"          -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )

  protected lazy val validSharedPayloads: Gen[JsObject] =
    for {
      epp_urn     <- nonEmptyAlphaNumStrings
      epp_account <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> "AbCd12345TFC",
      "epp_reg_reference"          -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )

  private lazy val validChildAccountRefs = for {
    letters <- Gen.stringOfN(CHILD_ACCOUNT_REF_LETTERS, Gen.alphaChar)
    digits  <- Gen.stringOfN(CHILD_ACCOUNT_REF_DIGITS, Gen.numChar)
  } yield s"$letters${digits}TFC"

  private lazy val invalidChildAccountRefs            = Gen.asciiPrintableStr.filterNot(_ matches EXPECTED_CHILD_ACCOUNT_REF_PATTERN)
  private lazy val EXPECTED_CHILD_ACCOUNT_REF_PATTERN = s"^[a-zA-Z]{$CHILD_ACCOUNT_REF_LETTERS}\\d{$CHILD_ACCOUNT_REF_DIGITS}TFC$$"

  private lazy val CHILD_ACCOUNT_REF_LETTERS = 4
  private lazy val CHILD_ACCOUNT_REF_DIGITS  = 5

  private lazy val nonAlphaNumStrings = Gen.asciiPrintableStr.map(_.filterNot(_.isLetterOrDigit))
}
