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

package models.request.external

import models.request.external.validation.*
import models.request.nsi.NsiPaymentRequest
import play.api.libs.json.{Json, Reads}

final case class ExternalPaymentRequest(
    outbound_child_payment_ref: ChildAccountRef,
    epp_reg_reference: NonEmptyAlphaNumString,
    epp_unique_customer_id: NonEmptyAlphaNumString,
    payment_amount: PositiveInt,
    payee_type: PayeeType,
    ccp_reg_reference: Urn,
    ccp_postcode: PostCode
) {

  def toNsiPaymentRequest: NsiPaymentRequest = NsiPaymentRequest(
    eppAccount = epp_unique_customer_id.toString,
    eppURN = epp_reg_reference.toString,
    amount = payment_amount.toInt,
    childAccountPaymentRef = outbound_child_payment_ref.toString,
    payeeType = payee_type,
    ccpURN = ccp_reg_reference.toString,
    ccpPostcode = ccp_postcode.toString
  )

}

object ExternalPaymentRequest {
  given Reads[ExternalPaymentRequest] = Json.reads[ExternalPaymentRequest]
}
