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
import helpers.generators.request.external.validation.{ChildAccountRefGenerators, NonEmptyAlphaNumStringGenerators}
import models.request.external.ExternalBalanceRequest
import org.scalacheck.Gen
import play.api.libs.json.JsObject

trait ExternalBalanceRequestGenerators
    extends ChildAccountRefGenerators
    with NonEmptyAlphaNumStringGenerators
    with JsonGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"

  protected val genExternalBalanceRequests: Gen[ExternalBalanceRequest] = for {
    outbound_child_payment_ref <- genChildAccountRefs
    epp_reg_reference          <- genNonEmptyAlphaNumStringValues
    epp_unique_customer_id     <- genNonEmptyAlphaNumStringValues
  } yield ExternalBalanceRequest(
    outbound_child_payment_ref = outbound_child_payment_ref,
    epp_reg_reference = epp_reg_reference,
    epp_unique_customer_id = epp_unique_customer_id
  )

  protected val genExternalBalanceRequestJsObjects: Gen[JsObject] = jsObjects(
    outbound_child_payment_ref -> genChildAccountRefStrings,
    epp_reg_reference          -> genNonEmptyAlphaNumStrings,
    epp_unique_customer_id     -> genNonEmptyAlphaNumStrings
  )

  protected val genExternalBalanceRequestJsObjectsWithoutOutboundChildPaymentRef: Gen[JsObject] =
    genExternalBalanceRequestJsObjects - outbound_child_payment_ref

  protected val genExternalBalanceRequestJsObjectsWithInvalidOutboundChildPaymentRef: Gen[JsObject] =
    genExternalBalanceRequestJsObjects + (outbound_child_payment_ref -> genInvalidChildAccountRefStrings)

  protected val genExternalBalanceRequestJsObjectsWithoutEppRegReference: Gen[JsObject] =
    genExternalBalanceRequestJsObjects - epp_reg_reference

  protected val genExternalBalanceRequestJsObjectsWithInvalidEppRegReference: Gen[JsObject] =
    genExternalBalanceRequestJsObjects + (epp_reg_reference -> genInvalidNonEmptyAlphaNumStrings)

  protected val genExternalBalanceRequestJsObjectsWithoutEppUniqueCustomerId: Gen[JsObject] =
    genExternalBalanceRequestJsObjects - epp_unique_customer_id

  protected val genExternalBalanceRequestJsObjectsWithInvalidEppUniqueCustomerId: Gen[JsObject] =
    genExternalBalanceRequestJsObjects + (epp_unique_customer_id -> genInvalidNonEmptyAlphaNumStrings)

}
