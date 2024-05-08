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

import play.api.libs.json.{Json, OWrites, Reads}

import java.util.UUID

final case class RequestMetadata(
    correlation_id: UUID,
    epp_unique_customer_id: String,
    epp_reg_reference: String,
    outbound_child_payment_ref: String
  )

object RequestMetadata {
  private val CUSTOMER_ID_PATTERN      = "^[0-9]{11}$"
  private val REGISTRATION_REF_PATTERN = "^[a-zA-Z0-9]{16}$"
  private val PAYMENT_REF_PATTERN      = "^[A-Z]{4}[0-9]{5}TFC$"

  implicit val reads: Reads[RequestMetadata] = Json.reads filter { br =>
    (br.epp_unique_customer_id matches CUSTOMER_ID_PATTERN) &&
    (br.epp_reg_reference matches REGISTRATION_REF_PATTERN) &&
    (br.outbound_child_payment_ref matches PAYMENT_REF_PATTERN)
  }

  implicit val writes: OWrites[RequestMetadata] = Json.writes
}
