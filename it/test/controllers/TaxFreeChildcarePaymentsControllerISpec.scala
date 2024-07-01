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
import ch.qos.logback.classic.Level
import config.CustomJsonErrorHandler
import org.scalatest.{Assertion, LoneElement}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.util.UUID

class TaxFreeChildcarePaymentsControllerISpec
    extends BaseISpec
    with NsiStubs
    with LogCapturing
    with LoneElement
    with JsonGenerators {

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

            val request = wsClient
              .url(s"$baseUrl/link")
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> expectedCorrelationId.toString
              )

            println("Request header: " + request.header(CORRELATION_ID))

            val response = request
              .post(randomLinkRequestJson)
              .futureValue

            println("Response header: " + response.header(CORRELATION_ID))

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

    "POST /" should {
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
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

          val res = wsClient
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

      s"respond with $BAD_REQUEST and generic error message" when {
        val expectedCorrelationId = UUID.randomUUID()

        s"payment amount is invalid" in withAuthNinoRetrievalExpectLog("payment", expectedCorrelationId.toString) {
          val invalidPaymentRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "payment_amount"             -> "I am a bad payment reference",
            "ccp_reg_reference"          -> "qwertyui",
            "ccp_postcode"               -> "AS12 3DF",
            "payee_type"                 -> randomPayeeType
          )

          val res = wsClient
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

  private def withAuthNinoRetrievalExpectLog(
      expectedEndpoint: String,
      expectedCorrelationId: String
    )(
      doTest: => Assertion
    ): Unit = {
    withCaptureOfLoggingFrom(
      Logger(classOf[CustomJsonErrorHandler])
    ) { logs =>
      withAuthNinoRetrieval {
        doTest
      }

      val log = logs.loneElement
      log.getLevel shouldBe Level.INFO
      log.getMessage match {
        case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
          loggedEndpoint shouldBe expectedEndpoint
          loggedCorrelationId shouldBe expectedCorrelationId
          loggedMessage should startWith("Json validation error")

        case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
      }
    }
  }
}
