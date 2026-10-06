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

package models.response.nsi

import models.response.external.ExternalPaymentResponse
import play.api.libs.json.{Json, Reads}

import java.time.LocalDate

case class NsiPaymentResponse(
    paymentReference: String,
    paymentDate: LocalDate
) {

  def toExternalPaymentResponse: ExternalPaymentResponse = ExternalPaymentResponse(
    payment_reference = paymentReference,
    estimated_payment_date = paymentDate
  )

}

object NsiPaymentResponse {
  given Reads[NsiPaymentResponse] = Json.reads[NsiPaymentResponse]
}
