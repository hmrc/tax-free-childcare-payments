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
import play.api.libs.json.{ConstraintReads, Json, OWrites, Reads, __}

final case class SharedRequestData(
    epp_unique_customer_id: String,
    epp_reg_reference: String,
    outbound_child_payment_ref: String
)

object SharedRequestData extends ConstraintReads {

  val TFC_ACCOUNT_REF_KEY = "outbound_child_payment_ref"
  val EPP_URN_KEY         = "epp_reg_reference"
  val EPP_ACCOUNT_ID_KEY  = "epp_unique_customer_id"

  private val NonEmptyAlphaNumStringReads: Reads[String] = pattern("[a-zA-Z0-9]{1,255}".r)
  private val TfcAccountRefReads: Reads[String]          = pattern("[a-zA-Z]{2}[a-zA-Z0'.\\- ]{2}[0-9]{5}TFC".r)

  implicit val readsFromUser: Reads[SharedRequestData] =
    (__ \ EPP_ACCOUNT_ID_KEY)
      .read[String](NonEmptyAlphaNumStringReads)
      .and((__ \ EPP_URN_KEY).read[String](NonEmptyAlphaNumStringReads))
      .and((__ \ TFC_ACCOUNT_REF_KEY).read[String](TfcAccountRefReads))(SharedRequestData.apply _)

  implicit val writes: OWrites[SharedRequestData] = srd =>
    Json.obj(
      "eppAccount" -> srd.epp_unique_customer_id,
      "eppURN"     -> srd.epp_reg_reference
    )

}
