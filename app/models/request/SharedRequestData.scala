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

package models.request

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
    (__ \ EPP_ACCOUNT_ID_KEY).read(NonEmptyAlphaNumStringReads) ~
      (__ \ EPP_URN_KEY).read(NonEmptyAlphaNumStringReads) ~
      (__ \ TFC_ACCOUNT_REF_KEY).read(TfcAccountRefReads)
  )(apply _)

  lazy val TFC_ACCOUNT_REF_KEY = "outbound_child_payment_ref"
  lazy val EPP_URN_KEY         = "epp_reg_reference"
  lazy val EPP_ACCOUNT_ID_KEY  = "epp_unique_customer_id"

  private lazy val NonEmptyAlphaNumStringReads = pattern("[a-zA-Z0-9]{1,255}".r)
  private lazy val TfcAccountRefReads          = pattern("[a-zA-Z]{2}[a-zA-Z0'.\\- ]{2}[0-9]{5}TFC".r)
}
