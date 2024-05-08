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

package models.requests

import models.requests.PaymentRequest.PayeeType
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, __}

final case class PaymentRequest(
    metadata: RequestMetadata,
    payment_amount: BigDecimal,
    ccp_reg_reference: String,
    ccp_postcode: String,
    payee_type: PayeeType.Value
  )

object PaymentRequest {

  object PayeeType extends Enumeration {
    val ccp, epp = Value
  }

  implicit val formatPayeeType: Format[PayeeType.Value] = Json.formatEnum(PayeeType)

  implicit val format: OFormat[PaymentRequest] = (
    __.format[RequestMetadata] ~
      (__ \ "payment_amount").format[BigDecimal] ~
      (__ \ "ccp_reg_reference").format[String] ~
      (__ \ "ccp_postcode").format[String] ~
      (__ \ "payee_type").format[PayeeType.Value]
  )(apply, unlift(unapply))
}
