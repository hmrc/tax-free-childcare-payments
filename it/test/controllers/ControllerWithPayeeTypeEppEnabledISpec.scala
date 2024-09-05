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

import base.{AuthStubs, BaseISpec, NsiStubs}
import models.request.data.Generators
import models.request.{IdentifierRequest, PaymentRequest}
import models.response.PaymentResponse
import play.api.libs.json.Json

class ControllerWithPayeeTypeEppEnabledISpec
    extends BaseISpec(enablePayeeTypeEPP = true)
    with AuthStubs
    with NsiStubs
    with Generators
    with models.response.Generators {
  "POST /" should {
    "respond 200 with expected body" when {
      "payment request is valid with payee type either CCP or EPP" in
        forAll { (request: IdentifierRequest[PaymentRequest], expectedResponse: PaymentResponse) =>
          stubAuthRetrievalOf(request.nino)
          stubNsiMakePayment201(getNsiJsonFrom(expectedResponse))

          val expectedCorrelationId   = request.correlation_id.toString
          val expectedTfcResponseBody = Json.toJson(expectedResponse)

          withClient { ws =>
            val response = ws
              .url(s"$baseUrl/")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId
              )
              .post(getJsonFrom(request.body))
              .futureValue

            checkSecurityPolicy(response)

            response.status                       shouldBe OK
            response.header(CORRELATION_ID).value shouldBe expectedCorrelationId
            response.json                         shouldBe expectedTfcResponseBody
          }
        }
    }
  }
}
