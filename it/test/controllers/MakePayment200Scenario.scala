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

package controllers

import base.Generators
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

final case class MakePayment200Scenario(
    nsiRequestJson: JsValue,
    expectedResponseJson: JsValue
  )

object MakePayment200Scenario extends Generators {
  import org.scalacheck.{Arbitrary, Gen}

  implicit val arb: Arbitrary[MakePayment200Scenario] = Arbitrary(
    for {
      expectedPaymentRef       <- nonEmptyAlphaNumStrings
      expectedPaymentDelayDays <- Gen.chooseNum(0, MAX_PAYMENT_DELAY_DAYS)
    } yield apply(
      Json.obj(),
      Json.obj(
        "payment_reference"      -> expectedPaymentRef,
        "estimated_payment_date" -> (LocalDate.now() plusDays expectedPaymentDelayDays)
      )
    )
  )

  private lazy val MAX_PAYMENT_DELAY_DAYS = 31
}
