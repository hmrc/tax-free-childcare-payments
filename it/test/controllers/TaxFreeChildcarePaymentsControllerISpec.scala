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

import base.{BaseISpec, JsonGenerators, NsiStubs}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import play.api.libs.json.{JsString, Json}

import java.util.UUID

class TaxFreeChildcarePaymentsControllerISpec extends BaseISpec with NsiStubs with JsonGenerators {
  withClient { wsClient =>
    "POST /link" should {
      s"respond with status $OK and correct JSON body" when {

        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in
          withAuthNinoRetrieval {
            val expectedChildName       = fullNames.sample.get
            val expectedCorrelationId   = UUID.randomUUID()
            val expectedTfcResponseBody = Json.obj("child_full_name" -> expectedChildName)
            val expectedNsiResponseBody = Json.obj("childFullName" -> expectedChildName)

            stubNsiLinkAccounts201(expectedNsiResponseBody)

            val request  = wsClient
              .url(s"$baseUrl/link")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )
            val response = request
              .post(randomLinkRequestJson)
              .futureValue

            val resCorrelationId = UUID fromString response.header(CORRELATION_ID).value

            response.status shouldBe OK
            resCorrelationId shouldBe expectedCorrelationId
            response.json shouldBe expectedTfcResponseBody
          }
      }

      s"respond with $BAD_REQUEST and generic error message" when {
        val expectedCorrelationId = UUID.randomUUID()

        s"child DOB is invalid" in withAuthNinoRetrievalExpectLog("link", expectedCorrelationId.toString) {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> "I am a bad date string"
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          res.body shouldBe EXPECTED_JSON_ERROR_RESPONSE.toString
        }
      }
    }

    "POST /balance" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId   = UUID.randomUUID()
          val expectedTfcResponseBody = Json.obj(
            "tfc_account_status" -> "ACTIVE",
            "paid_in_by_you"     -> 0,
            "government_top_up"  -> 0,
            "total_balance"      -> 0,
            "cleared_funds"      -> 0,
            "top_up_allowance"   -> 0
          )
          val expectedNsiResponseBody = Json.obj(
            "accountStatus"  -> "ACTIVE",
            "topUpAvailable" -> 0,
            "topUpRemaining" -> 0,
            "paidIn"         -> 0,
            "totalBalance"   -> 0,
            "clearedFunds"   -> 0
          )

          stubNsiCheckBalance200(expectedNsiResponseBody)

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(validCheckBalanceRequestPayloads.sample.get)
            .futureValue

          val resCorrelationId = UUID fromString res.header(CORRELATION_ID).value

          res.status shouldBe OK
          resCorrelationId shouldBe expectedCorrelationId
          res.json shouldBe expectedTfcResponseBody
        }
      }
    }
  }

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

  private val endpoints = Table(
    ("Name", "TFC URL", "NSI Mapping", "Valid Payload"),
    ("link", "/link", nsiLinkAccountsEndpoint, randomLinkRequestJson),
    ("balance", "/balance", nsiCheckBalanceEndpoint, randomSharedJson),
    ("payment", "/", nsiMakePaymentEndpoint, randomPaymentRequestJson)
  )

  private val nsiErrorScenarios = Table(
    ("NSI Status Code", "NSI Error Code", "Expected Status Code"),
    (BAD_REQUEST, "E0000", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0001", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0002", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0003", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0004", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0005", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0006", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0007", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0008", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0020", BAD_GATEWAY),
    (BAD_REQUEST, "E0021", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0022", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0023", INTERNAL_SERVER_ERROR),
    (BAD_REQUEST, "E0024", BAD_REQUEST),
    (BAD_REQUEST, "E0025", BAD_REQUEST),
    (BAD_REQUEST, "E0026", BAD_REQUEST),
    (UNAUTHORIZED, "E0401", INTERNAL_SERVER_ERROR),
    (FORBIDDEN, "E0030", BAD_REQUEST),
    (FORBIDDEN, "E0031", BAD_REQUEST),
    (FORBIDDEN, "E0032", BAD_REQUEST),
    (FORBIDDEN, "E0033", BAD_REQUEST),
    (FORBIDDEN, "E0034", SERVICE_UNAVAILABLE),
    (FORBIDDEN, "E0035", BAD_REQUEST),
    (NOT_FOUND, "E0040", BAD_REQUEST),
    (NOT_FOUND, "E0041", BAD_REQUEST),
    (NOT_FOUND, "E0042", BAD_REQUEST),
    (NOT_FOUND, "E0043", BAD_REQUEST),
    (INTERNAL_SERVER_ERROR, "E9000", SERVICE_UNAVAILABLE),
    (INTERNAL_SERVER_ERROR, "E9999", SERVICE_UNAVAILABLE),
    (SERVICE_UNAVAILABLE, "E8000", SERVICE_UNAVAILABLE),
    (SERVICE_UNAVAILABLE, "E8001", SERVICE_UNAVAILABLE)
  )

  private val sharedBadRequestScenarios = Table(
    ("Spec", "Field", "Bad Value"),
    ("customer ID is invalid", "epp_unique_customer_id", "I am a bad customer ID."),
    ("registration ref is invalid", "epp_reg_reference", "I am a bad registration reference"),
    ("payment ref is invalid", "outbound_child_payment_ref", "I am a bad payment reference.")
  )

  forAll(endpoints) { (name, tfc_url, nsiMapping, validPayload) =>
    s"POST $tfc_url" should {
      s"respond with $BAD_REQUEST and generic error message" when {
        forAll(sharedBadRequestScenarios) { (spec, field, badValue) =>
          val expectedCorrelationId = UUID.randomUUID().toString

          spec in withClient { ws =>
            val invalidPayload = validPayload + (field -> JsString(badValue))

            withAuthNinoRetrievalExpectLog(name, expectedCorrelationId) {
              val res = ws
                .url(s"$baseUrl$tfc_url")
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> expectedCorrelationId
                )
                .post(invalidPayload)
                .futureValue

              res.status shouldBe BAD_REQUEST
              res.json shouldBe EXPECTED_JSON_ERROR_RESPONSE
            }
          }
        }
      }

      forAll(nsiErrorScenarios) {
        (nsiStatusCode, nsiErrorCode, expectedStatusCode) =>
          s"respond with status $expectedStatusCode" when {
            s"NSI responds status code $nsiStatusCode and errorCode $nsiErrorCode" in withClient { ws =>
              withAuthNinoRetrieval {
                val nsiResponseBody = Json.obj("errorCode" -> nsiErrorCode)
                val nsiResponse     = aResponse().withStatus(nsiStatusCode).withBody(nsiResponseBody.toString)
                stubFor(nsiMapping willReturn nsiResponse)

                val response = ws
                  .url(s"$baseUrl$tfc_url")
                  .withHttpHeaders(
                    AUTHORIZATION  -> "Bearer qwertyuiop",
                    CORRELATION_ID -> UUID.randomUUID().toString
                  )
                  .post(validPayload)
                  .futureValue

                response.status shouldBe expectedStatusCode
              }
            }
          }
      }
    }
  }
}
