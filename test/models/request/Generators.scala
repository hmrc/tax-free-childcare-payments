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

import models.request.Payee.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY}
import models.request.PaymentRequest.PAYMENT_AMOUNT_KEY
import play.api.libs.json._
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.time.{LocalDate, ZoneId}

trait Generators extends base.Generators with RandomPayeeJson {
  import org.scalacheck.{Arbitrary, Gen}
  import Arbitrary.arbitrary

  protected implicit def arbIdentifierRequest[A: Arbitrary]: Arbitrary[IdentifierRequest[A]] = Arbitrary(
    randomIdentifierRequest(arbitrary[A])
  )

  protected def randomIdentifierRequest[A](randomBody: Gen[A]): Gen[IdentifierRequest[A]] = for {
    nino          <- randomNinos
    correlationId <- Gen.uuid
    body          <- randomBody
  } yield IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), body))

  /** START Link Accounts generators */

  protected implicit val arbLinkRequest: Arbitrary[LinkRequest] = Arbitrary(
    for {
      sharedRequestData <- validSharedDataModels
      calendar          <- Gen.calendar
      childDateOfBirth   = calendar.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
      childYearOfBirth  <- Gen.chooseNum(2000, 3000)
    } yield LinkRequest(
      sharedRequestData,
      childDateOfBirth withYear childYearOfBirth
    )
  )

  protected val validLinkPayloads: Gen[JsObject] = linkPayloadsWith(validSharedJson)

  protected lazy val linkPayloadsWithMissingTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingTfcAccountRef)

  protected lazy val linkPayloadsWithInvalidTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidTfcAccountRef)

  protected lazy val linkPayloadsWithMissingEppUrn: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingEppUrn)

  protected lazy val linkPayloadsWithInvalidEppUrn: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidEppUrn)

  protected lazy val linkPayloadsWithMissingEppAccountId: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingEppAccountId)

  protected lazy val linkPayloadsWithInvalidEppAccountId: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidEppAccountId)

  protected lazy val linkPayloadsWithMissingChildDob: Gen[JsValue] = validSharedJson

  protected lazy val linkPayloadsWithNonStringChildDob: Gen[JsObject] = for {
    sharedPayload  <- validSharedJson
    nonStringValue <- Gen.long map { num => JsNumber(num) }
  } yield sharedPayload + ("child_date_of_birth" -> nonStringValue)

  protected lazy val linkPayloadsWithNonIso8061ChildDob: Gen[JsObject] = for {
    sharedPayload   <- validSharedJson
    nonIso8061Value <- Gen.alphaNumStr map JsString.apply
  } yield sharedPayload + ("child_date_of_birth" -> nonIso8061Value)

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
  protected implicit val arbSharedRequestData: Arbitrary[SharedRequestData] = Arbitrary(validSharedDataModels)

  protected val validCheckBalanceRequestPayloads: Gen[JsObject] = validSharedJson

  /** END Check Balance generators
    *
    * BEGIN Make Payment generators
    */
  protected implicit val arbPaymentRequest: Arbitrary[PaymentRequest] = Arbitrary(
    randomPaymentRequestWith(arbitrary[SharedRequestData], randomPayees)
  )

  protected def randomPaymentRequestWithOnlyCCP: Gen[PaymentRequest] =
    randomPaymentRequestWith(randomPayees = randomChildCareProviders)

  protected def randomPaymentRequestWith(
      randomSharedData: Gen[SharedRequestData] = arbitrary[SharedRequestData],
      randomPayees: Gen[Payee]
    ): Gen[PaymentRequest] = for {
    sharedRequestData  <- randomSharedData
    payee              <- randomPayees
    paymentAmountPence <- Gen.posNum[Int]
  } yield PaymentRequest(sharedRequestData, paymentAmountPence, payee)

  protected def getJsonFrom(request: PaymentRequest): JsObject =
    getJsonFrom(request.sharedRequestData) ++ getJsonFrom(request.payee) ++ Json.obj(
      PAYMENT_AMOUNT_KEY -> request.payment_amount
    )

  protected val randomPaymentJsonWithMissingPayeeType: Gen[JsObject] = validPaymentJsonWithAnyPayee.map(_ - PAYEE_TYPE_KEY)

  /** BEGIN Random Payment JSON with Any Payee. */

  protected lazy val validPaymentJsonWithAnyPayee: Gen[JsObject] = arbitrary[PaymentRequest] map getJsonFrom

  protected val randomPaymentJsonWithAnyPayeeAndMissingTfcAccountRef: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithMissingTfcAccountRef)

  protected val randomPaymentJsonWithAnyPayeeAndInvalidTfcAccountRef: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithInvalidTfcAccountRef)

  protected val randomPaymentJsonWithAnyPayeeAndMissingEppUrn: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithMissingEppUrn)

  protected val randomPaymentJsonWithAnyPayeeAndInvalidEppUrn: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithInvalidEppUrn)

  protected val randomPaymentJsonWithAnyPayeeAndMissingEppAccountId: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithMissingEppAccountId)

  protected val randomPaymentJsonWithAnyPayeeAndInvalidEppAccountId: Gen[JsObject] = randomPaymentJsonWithAnyPayeeAnd(sharedPayloadsWithInvalidEppAccountId)

  protected val randomPaymentJsonWithInvalidPayeeType: Gen[JsObject] = for {
    paymentPayload   <- validPaymentJsonWithAnyPayee
    invalidPayeeType <- invalidPayeeTypes
  } yield paymentPayload + (PAYEE_TYPE_KEY -> invalidPayeeType)

  protected val randomPaymentJsonWithAnyPayeeAndMissingPaymentAmount: Gen[JsObject] = validPaymentJsonWithAnyPayee.map(_ - PAYMENT_AMOUNT_KEY)

  protected val randomPaymentJsonWithAnyPayeeAndFractionalPaymentAmount: Gen[JsObject] = for {
    paymentPayload          <- validPaymentJsonWithAnyPayee
    fractionalPaymentAmount <- Gen.double if !fractionalPaymentAmount.isValidInt
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsNumber(fractionalPaymentAmount))

  protected val randomPaymentJsonWithAnyPayeeAndStringPaymentAmount: Gen[JsObject] = for {
    paymentPayload          <- validPaymentJsonWithAnyPayee
    fractionalPaymentAmount <- Gen.asciiPrintableStr
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsString(fractionalPaymentAmount))

  protected val randomPaymentJsonWithAnyPayeeAndNonPositivePaymentAmount: Gen[JsObject] = for {
    paymentPayload     <- validPaymentJsonWithAnyPayee
    nonPositivePayment <- Gen.oneOf(Gen const 0, Gen.negNum[Int])
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsNumber(nonPositivePayment))

  private def randomPaymentJsonWithAnyPayeeAnd(randomSharedJson: Gen[JsObject]) =
    for {
      sharedJson         <- randomSharedJson
      payeeJson          <- validPayeeJson
      paymentAmountPence <- Gen.posNum[Int]
    } yield sharedJson ++ payeeJson ++ Json.obj(
      PAYMENT_AMOUNT_KEY -> paymentAmountPence
    )

  /** END Random Payment JSON with Any Payee.
    *
    * BEGIN Random Payment JSON with CCP Only.
    */
  protected val validPaymentRequestWithPayeeTypeSetToCCP: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(validSharedJson)

  protected val randomPaymentJsonWithCcpOnlyAndMissingTfcAccountRef: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithMissingTfcAccountRef)

  protected val randomPaymentJsonWithCcpOnlyAndInvalidTfcAccountRef: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithInvalidTfcAccountRef)

  protected val randomPaymentJsonWithCcpOnlyAndMissingEppUrn: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithMissingEppUrn)

  protected val randomPaymentJsonWithCcpOnlyAndInvalidEppUrn: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithInvalidEppUrn)

  protected val randomPaymentJsonWithCcpOnlyAndMissingEppAccountId: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithMissingEppAccountId)

  protected val randomPaymentJsonWithCcpOnlyAndInvalidEppAccountId: Gen[JsObject] = randomPaymentJsonWithCcpOnlyAnd(sharedPayloadsWithInvalidEppAccountId)

  protected val randomPaymentJsonWithPayeeTypeNotCCP: Gen[JsObject] = for {
    paymentPayload   <- validPaymentJsonWithAnyPayee
    invalidPayeeType <- Gen.oneOf(Gen const JsString("EPP"), invalidPayeeTypes)
  } yield paymentPayload + ("payee_type" -> invalidPayeeType)

  protected val randomPaymentJsonWithCcpOnlyAndMissingCcpUrn: Gen[JsObject] = validPaymentRequestWithPayeeTypeSetToCCP.map(_ - CCP_URN_KEY)

  protected val randomPaymentJsonWithCcpOnlyAndInvalidCcpUrn: Gen[JsObject] = for {
    paymentPayload <- validPaymentRequestWithPayeeTypeSetToCCP
    invalidCcpUrn  <- invalidCcpUrns
  } yield paymentPayload + (CCP_URN_KEY -> invalidCcpUrn)

  protected val randomPaymentJsonWithCcpOnlyAndMissinCcpPostcode: Gen[JsObject] = validPaymentRequestWithPayeeTypeSetToCCP.map(_ - CCP_POSTCODE_KEY)

  protected val randomPaymentJsonWithCcpOnlyAndInvalidCcpPostcode: Gen[JsObject] = for {
    paymentPayload     <- validPaymentRequestWithPayeeTypeSetToCCP
    invalidCcpPostcode <- nonAlphaNumStrings
  } yield paymentPayload + (CCP_POSTCODE_KEY -> JsString(invalidCcpPostcode))

  protected val randomPaymentJsonWithCcpOnlyAndMissingPaymentAmount: Gen[JsObject] = validPaymentRequestWithPayeeTypeSetToCCP.map(_ - PAYMENT_AMOUNT_KEY)

  protected val randomPaymentJsonWithCcpOnlyAndFractionalPaymentAmount: Gen[JsObject] = for {
    paymentPayload          <- validPaymentRequestWithPayeeTypeSetToCCP
    fractionalPaymentAmount <- Gen.double if !fractionalPaymentAmount.isValidInt
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsNumber(fractionalPaymentAmount))

  protected val randomPaymentJsonWithCcpOnlyAndStringPaymentAmount: Gen[JsObject] = for {
    paymentPayload          <- validPaymentRequestWithPayeeTypeSetToCCP
    fractionalPaymentAmount <- Gen.asciiPrintableStr
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsString(fractionalPaymentAmount))

  protected val randomPaymentJsonWithCcpOnlyAndNonPositivePaymentAmount: Gen[JsObject] = for {
    paymentPayload     <- validPaymentRequestWithPayeeTypeSetToCCP
    nonPositivePayment <- Gen.oneOf(Gen const 0, Gen.negNum[Int])
  } yield paymentPayload + (PAYMENT_AMOUNT_KEY -> JsNumber(nonPositivePayment))

  private def randomPaymentJsonWithCcpOnlyAnd(randomSharedJson: Gen[JsObject]) =
    for {
      sharedJson         <- randomSharedJson
      ccpJson            <- validCcpJson
      paymentAmountPence <- Gen.posNum[Int]
    } yield sharedJson ++ ccpJson ++ Json.obj(
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

  protected lazy val validSharedJson: Gen[JsObject] =
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
