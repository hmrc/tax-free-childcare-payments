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

package models.response

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, OWrites, Reads, __}

import java.time.LocalDate

final case class PaymentResponse(
    payment_reference: String,
    estimated_payment_date: LocalDate
)

object PaymentResponse {

  implicit val writesToUser: OWrites[PaymentResponse] = pr =>
    Json.obj(
      "payment_reference"      -> pr.payment_reference,
      "estimated_payment_date" -> pr.estimated_payment_date
    )

  implicit val readsFromNsi: Reads[PaymentResponse] =
    (__ \ "paymentReference").read[String].and((__ \ "paymentDate").read[LocalDate])(PaymentResponse.apply _)

}
