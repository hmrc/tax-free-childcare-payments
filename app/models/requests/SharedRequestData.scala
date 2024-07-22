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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Reads.StringReads
import play.api.libs.json._

final case class SharedRequestData(
    epp_unique_customer_id: String,
    epp_reg_reference: String,
    outbound_child_payment_ref: String
  )

object SharedRequestData extends ConstraintReads {

  implicit val readsFromApi: Reads[SharedRequestData] = (
    (__ \ "epp_unique_customer_id").read(NonEmptyAlphaNumStringReads) ~
      (__ \ "epp_reg_reference").read(NonEmptyAlphaNumStringReads) ~
      (__ \ "outbound_child_payment_ref").read(TfcAccountRefReads)
  )(apply _)

  private lazy val NonEmptyAlphaNumStringReads = pattern("[a-zA-Z0-9]{1,255}".r)
  private lazy val TfcAccountRefReads          = pattern("[a-zA-Z]{4}[0-9]{5}TFC".r)
}
