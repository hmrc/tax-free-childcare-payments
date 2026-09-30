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

package models.response.nsi

import helpers.BaseSpec
import models.response.external.ExternalPaymentResponse
import play.api.libs.json.{JsString, JsSuccess, Json}

import java.time.LocalDate

class NsiPaymentResponseSpec extends BaseSpec {

  private val nsiPaymentResponseJson = Json.obj(
    "paymentReference" -> JsString("paymentReference"),
    "paymentDate"      -> JsString("2026-09-30")
  )

  private val nsiPaymentResponse = NsiPaymentResponse(
    paymentReference = "paymentReference",
    paymentDate = LocalDate.of(2026, 9, 30)
  )

  private val externalPaymentResponse = ExternalPaymentResponse(
    payment_reference = "paymentReference",
    estimated_payment_date = LocalDate.of(2026, 9, 30)
  )

  "reads" should {
    "correctly parse from valid JSON" in {
      nsiPaymentResponseJson.validate[NsiPaymentResponse] shouldBe JsSuccess(nsiPaymentResponse)
    }
  }

  "toExternalPaymentResponse" should {
    "return the correct data" in {
      nsiPaymentResponse.toExternalPaymentResponse shouldBe externalPaymentResponse
    }
  }

}
