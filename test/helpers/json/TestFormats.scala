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

package helpers.json

import models.request.external.validation.{ChildAccountRef, Date, NonEmptyAlphaNumString, PositiveInt, PostCode, Urn}
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import models.response.nsi.{NsiAccountStatus, NsiBalanceResponse, NsiLinkResponse, NsiPaymentResponse}
import play.api.libs.json.{JsNumber, JsString, Json, Writes}

trait TestFormats {

  given Writes[NsiAccountStatus] = {
    case NsiAccountStatus.ACTIVE  => JsString("ACTIVE")
    case NsiAccountStatus.BLOCKED => JsString("BLOCKED")
  }

  given Writes[ChildAccountRef] = ref => JsString(ref.toString)

  given Writes[NonEmptyAlphaNumString] = string => JsString(string.toString)

  given Writes[Date] = date => JsString(date.toLocalDate.toString)

  given Writes[PostCode] = postCode => JsString(postCode.toString)

  given Writes[Urn] = urn => JsString(urn.toString)

  given Writes[PositiveInt] = int => JsNumber(int.toInt)

  given Writes[NsiLinkResponse] = Json.writes[NsiLinkResponse]

  given Writes[NsiPaymentResponse] = Json.writes[NsiPaymentResponse]

  given Writes[NsiBalanceResponse] = Json.writes[NsiBalanceResponse]

  given Writes[ExternalLinkRequest] = Json.writes[ExternalLinkRequest]

  given Writes[ExternalPaymentRequest] = Json.writes[ExternalPaymentRequest]

  given Writes[ExternalBalanceRequest] = Json.writes[ExternalBalanceRequest]

}
