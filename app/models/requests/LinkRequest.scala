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

import play.api.libs.json.{Json, Reads}

import java.time.LocalDate
import java.util.UUID

final case class LinkRequest(
    correlationId: UUID,
    epp_unique_customer_id: String,
    epp_reg_reference: String,
    outbound_child_payment_ref: String,
    child_date_of_birth: LocalDate
  )

object LinkRequest {
  private val CUSTOMER_ID_PATTERN = s"^[0-9]{11}$$"
  private val PAYMENT_REF_PATTERN = s"^[A-Z]{4}[0-9]{5}TFC$$"

  implicit val reads: Reads[LinkRequest] =
    Json.format filter { lr =>
      (lr.epp_unique_customer_id matches CUSTOMER_ID_PATTERN) &&
      (lr.outbound_child_payment_ref matches PAYMENT_REF_PATTERN)
    }
}
