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

import controllers.TaxFreeChildcarePaymentsController.pattern
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Reads, __}

final case class SharedRequestData(
    epp_unique_customer_id: String,
    epp_reg_reference: String,
    outbound_child_payment_ref: String
  )

object SharedRequestData {

  implicit val readsFromApi: Reads[SharedRequestData] = (
    (__ \ "epp_unique_customer_id").read(NON_EMPTY_ALPHA_NUM_STR_PATTERN) ~
      (__ \ "epp_reg_reference").read(NON_EMPTY_ALPHA_NUM_STR_PATTERN) ~
      (__ \ "outbound_child_payment_ref").read(TFC_FORMAT)
  )(apply _)

  lazy private val NON_EMPTY_ALPHA_NUM_STR_PATTERN = pattern("[a-zA-Z0-9]{1,255}".r)
  lazy private val TFC_FORMAT                      = pattern("[a-zA-Z]{4}[0-9]{5}TFC".r)
}
