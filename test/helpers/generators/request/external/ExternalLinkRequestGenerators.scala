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

package helpers.generators.request.external

import helpers.generators.JsonGenerators
import helpers.generators.request.external.validation.{
  ChildAccountRefGenerators,
  DateGenerators,
  NonEmptyAlphaNumStringGenerators
}
import models.request.external.ExternalLinkRequest
import org.scalacheck.Gen
import play.api.libs.json.*

trait ExternalLinkRequestGenerators
    extends ChildAccountRefGenerators
    with NonEmptyAlphaNumStringGenerators
    with DateGenerators
    with JsonGenerators {

  private val outbound_child_payment_ref = "outbound_child_payment_ref"
  private val epp_reg_reference          = "epp_reg_reference"
  private val epp_unique_customer_id     = "epp_unique_customer_id"
  private val child_date_of_birth        = "child_date_of_birth"

  protected val genExternalLinkRequests: Gen[ExternalLinkRequest] = for {
    outbound_child_payment_ref <- genChildAccountRefs
    epp_reg_reference          <- genNonEmptyAlphaNumStringValues
    epp_unique_customer_id     <- genNonEmptyAlphaNumStringValues
    child_date_of_birth        <- genDates
  } yield ExternalLinkRequest(
    outbound_child_payment_ref = outbound_child_payment_ref,
    epp_reg_reference = epp_reg_reference,
    epp_unique_customer_id = epp_unique_customer_id,
    child_date_of_birth = child_date_of_birth
  )

  protected val genExternalLinkRequestJsObjects: Gen[JsObject] = jsObjects(
    outbound_child_payment_ref -> genChildAccountRefStrings,
    epp_reg_reference          -> genNonEmptyAlphaNumStrings,
    epp_unique_customer_id     -> genNonEmptyAlphaNumStrings,
    child_date_of_birth        -> genIso8601DateStrings
  )

  protected val genExternalLinkRequestJsObjectsWithoutOutboundChildPaymentRef: Gen[JsObject] =
    genExternalLinkRequestJsObjects - outbound_child_payment_ref

  protected val genExternalLinkRequestJsObjectsWithInvalidOutboundChildPaymentRef: Gen[JsObject] =
    genExternalLinkRequestJsObjects + (outbound_child_payment_ref -> genInvalidChildAccountRefStrings)

  protected val genExternalLinkRequestJsObjectsWithoutEppRegReference: Gen[JsObject] =
    genExternalLinkRequestJsObjects - epp_reg_reference

  protected val genExternalLinkRequestJsObjectsWithInvalidEppRegReference: Gen[JsObject] =
    genExternalLinkRequestJsObjects + (epp_reg_reference -> genInvalidNonEmptyAlphaNumStrings)

  protected val genExternalLinkRequestJsObjectsWithoutEppUniqueCustomerId: Gen[JsObject] =
    genExternalLinkRequestJsObjects - epp_unique_customer_id

  protected val genExternalLinkRequestJsObjectsWithInvalidEppUniqueCustomerId: Gen[JsObject] =
    genExternalLinkRequestJsObjects + (epp_unique_customer_id -> genInvalidNonEmptyAlphaNumStrings)

  protected val genExternalLinkRequestJsObjectsWithoutChildDateOfBirth: Gen[JsObject] =
    genExternalLinkRequestJsObjects - child_date_of_birth

  protected val genExternalLinkRequestJsObjectsWithNonStringChildDateOfBirth: Gen[JsObject] =
    genExternalLinkRequestJsObjects + (child_date_of_birth -> genNonStringDates)

  protected val genExternalLinkRequestJsObjectsWithNonIso8601ChildDateOfBirth: Gen[JsObject] =
    genExternalLinkRequestJsObjects + (child_date_of_birth -> genInvalidIso8601DateStrings)

}
