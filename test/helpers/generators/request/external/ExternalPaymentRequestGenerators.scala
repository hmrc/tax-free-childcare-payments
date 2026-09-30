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

package helpers.generators.request.external

import helpers.generators.JsonGenerators
import helpers.generators.request.external.validation.{
  ChildAccountRefGenerators,
  NonEmptyAlphaNumStringGenerators,
  PayeeTypeGenerators,
  PositiveIntGenerators,
  PostCodeGenerators,
  UrnGenerators
}
import models.request.external.ExternalPaymentRequest
import org.scalacheck.Gen
import play.api.libs.json.JsObject

trait ExternalPaymentRequestGenerators
    extends ChildAccountRefGenerators
    with NonEmptyAlphaNumStringGenerators
    with PostCodeGenerators
    with PayeeTypeGenerators
    with PositiveIntGenerators
    with UrnGenerators
    with JsonGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"
  private val payment_amount             = "payment_amount"
  private val payee_type                 = "payee_type"
  private val ccp_reg_reference          = "ccp_reg_reference"
  private val ccp_postcode               = "ccp_postcode"

  protected val genExternalPaymentRequests: Gen[ExternalPaymentRequest] = for {
    outbound_child_payment_ref <- genChildAccountRefs
    epp_reg_reference          <- genNonEmptyAlphaNumStringValues
    epp_unique_customer_id     <- genNonEmptyAlphaNumStringValues
    payment_amount             <- genPositiveIntValues
    payee_type                 <- genPayeeTypes
    ccp_reg_reference          <- genUrns
    ccp_postcode               <- genPostCodes
  } yield ExternalPaymentRequest(
    outbound_child_payment_ref = outbound_child_payment_ref,
    epp_reg_reference = epp_reg_reference,
    epp_unique_customer_id = epp_unique_customer_id,
    payment_amount = payment_amount,
    payee_type = payee_type,
    ccp_reg_reference = ccp_reg_reference,
    ccp_postcode = ccp_postcode
  )

  protected val genExternalPaymentRequestJsObjects: Gen[JsObject] = jsObjects(
    outbound_child_payment_ref -> genChildAccountRefStrings,
    epp_reg_reference          -> genNonEmptyAlphaNumStrings,
    epp_unique_customer_id     -> genNonEmptyAlphaNumStrings,
    payment_amount             -> genPositiveInts,
    payee_type                 -> genPayeeTypeStrings,
    ccp_reg_reference          -> genUrnStrings,
    ccp_postcode               -> genPostCodeStrings
  )

  protected val genExternalPaymentRequestJsObjectsWithoutOutboundChildPaymentRef: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - outbound_child_payment_ref

  protected val genExternalPaymentRequestJsObjectsWithInvalidOutboundChildPaymentRef: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (outbound_child_payment_ref -> genInvalidChildAccountRefStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutEppRegReference: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - epp_reg_reference

  protected val genExternalPaymentRequestJsObjectsWithInvalidEppRegReference: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (epp_reg_reference -> genInvalidNonEmptyAlphaNumStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutEppUniqueCustomerId: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - epp_unique_customer_id

  protected val genExternalPaymentRequestJsObjectsWithInvalidEppUniqueCustomerId: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (epp_unique_customer_id -> genInvalidNonEmptyAlphaNumStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutPaymentAmount: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - payment_amount

  protected val genExternalPaymentRequestJsObjectsWithNegativePaymentAmount: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (payment_amount -> genNegativeInts)

  protected val genExternalPaymentRequestJsObjectsWithFractionalPaymentAmount: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (payment_amount -> genFractionalDoubles)

  protected val genExternalPaymentRequestJsObjectsWithStringPaymentAmount: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (payment_amount -> genPositiveIntStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutPayeeType: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - payee_type

  protected val genExternalPaymentRequestJsObjectsWithInvalidPayeeType: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (payee_type -> genInvalidPayeeTypeStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutCcpRegReference: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - ccp_reg_reference

  protected val genExternalPaymentRequestJsObjectsWithInvalidCcpRegReference: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (ccp_reg_reference -> genInvalidUrnStrings)

  protected val genExternalPaymentRequestJsObjectsWithoutCcpPostcode: Gen[JsObject] =
    genExternalPaymentRequestJsObjects - ccp_postcode

  protected val genExternalPaymentRequestJsObjectsWithInvalidCcpPostcode: Gen[JsObject] =
    genExternalPaymentRequestJsObjects + (ccp_postcode -> genInvalidPostCodeStrings)

}
