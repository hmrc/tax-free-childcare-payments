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

package models.request.data

import models.request.Payee.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY}
import models.request.PaymentRequest.PAYMENT_AMOUNT_KEY
import models.request.{Payee, PaymentRequest, SharedRequestData}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}

trait PaymentRequestGenerators extends SharedRequestGenerators with PayeeGenerators {

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
}
