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

import base.{BaseISpec, NsiStubs}
import play.api.libs.json.Json

import java.util.UUID

class PayeeTypeEPPOnControllerISpec extends BaseISpec with NsiStubs {
  "POST /" should {
    s"respond $OK" when {
      s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withClient { ws =>
        withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedTfcResponseBody = Json.obj(
            "payment_reference"      -> expectedPaymentRef,
            "estimated_payment_date" -> expectedPaymentDate
          )
          val expectedNsiResponseBody = Json.obj(
            "paymentReference" -> expectedPaymentRef,
            "paymentDate"      -> expectedPaymentDate
          )

          stubNsiMakePayment201(expectedNsiResponseBody)

          val res = ws
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomPaymentRequestJson)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedTfcResponseBody
        }
      }
    }

    s"respond with $BAD_REQUEST and generic error message" when {
      val expectedCorrelationId = UUID.randomUUID()

      s"payment amount is invalid" in withClient { ws =>
        withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
          val invalidPaymentRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "payment_amount"             -> "I am a bad payment reference",
            "ccp_reg_reference"          -> "qwertyui",
            "ccp_postcode"               -> "AS12 3DF",
            "payee_type"                 -> randomPayeeType
          )

          val res = ws
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(invalidPaymentRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }
      }
    }
  }
}
