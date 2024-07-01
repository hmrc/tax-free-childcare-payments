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

import models.requests.PaymentRequest.ChildCareProvider

final case class PaymentRequest(
    sharedRequestData: SharedRequestData,
    payment_amount: Int,
    opt_ccp: Option[ChildCareProvider]
  )

object PaymentRequest {
  import play.api.libs.functional.syntax.toFunctionalBuilderOps
  import play.api.libs.json._

  final case class ChildCareProvider(urn: String, postcode: String)

  object ChildCareProvider {

    implicit val reads: Reads[ChildCareProvider] = (
      (__ \ "ccp_reg_reference").read[String] ~
        (__ \ "ccp_postcode").read[String]
    )(apply _)
  }

  object PayeeType extends Enumeration {
    val CCP, EPP = Value

    implicit val format: Format[PayeeType.Value] = Json.formatEnum(this)
  }
}
