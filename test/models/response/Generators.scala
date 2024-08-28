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

import java.time.ZoneId

import org.scalacheck.{Arbitrary, Gen}

import play.api.libs.json.{JsObject, Json}

trait Generators extends base.Generators {

  implicit protected val arbLinkResponse: Arbitrary[LinkResponse] = Arbitrary(
    fullNames map LinkResponse.apply
  )

  protected def getNsiJsonFrom(response: LinkResponse): JsObject = Json.obj(
    "childFullName" -> response.childFullName
  )

  implicit protected val arbPaymentResponse: Arbitrary[PaymentResponse] = Arbitrary(
    for {
      reference <- Gen.asciiPrintableStr
      calendar  <- Gen.calendar
      date       = calendar.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
    } yield PaymentResponse(reference, date)
  )

  protected def getNsiJsonFrom(response: PaymentResponse): JsObject = Json.obj(
    "paymentReference" -> response.payment_reference,
    "paymentDate"      -> response.estimated_payment_date
  )

  protected val randomUnknownErrorCodes: Gen[String] = Gen.oneOf(
    Gen.alphaStr,
    Gen.numStr,
    Gen const "UNKNOWN",
    Gen const "E0040"
  )
}
