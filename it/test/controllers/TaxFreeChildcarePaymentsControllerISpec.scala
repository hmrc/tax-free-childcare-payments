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

import base.BaseSpec
import org.scalatest.Assertion
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

import java.util.UUID

class TaxFreeChildcarePaymentsControllerISpec
    extends BaseSpec
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status {

  import com.github.tomakehurst.wiremock.client.WireMock._
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.json.Json
  import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort
    ).build()

  withClient { wsClient =>
    val contextRoot = "/individuals/tax-free-childcare/payments"
    val baseUrl     = s"http://localhost:$port$contextRoot"

    /** Covers [[TaxFreeChildcarePaymentsController.link()]] */
    "POST /link" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedResponseJson  = Json.obj(
            "child_full_name" -> "Peter Pan"
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/link")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponseJson.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomLinkRequestJson)
            .futureValue

          res.status shouldBe OK
          res.header(CORRELATION_ID).value shouldBe expectedCorrelationId
          res.json shouldBe expectedResponseJson
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {

        s"customer ID is invalid" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> "I am a bad customer ID.",
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> randomDateOfBirth
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }

        s"registration ref is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> "I am a bad registration reference",
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> randomDateOfBirth
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }

        s"payment ref is invalid" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> "I am a bad payment reference.",
            "child_date_of_birth"        -> randomDateOfBirth
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }

        s"child DOB is invalid" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> "I am a bad date string"
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }
      }
    }

    /** Covers [[TaxFreeChildcarePaymentsController.balance()]] */
    "POST /balance" should {
      s"respond with status $OK and correct JSON body" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedResponse      = Json.obj(
            "tfc_account_status" -> "active",
            "paid_in_by_you"     -> 0,
            "government_top_up"  -> 0,
            "total_balance"      -> 0,
            "cleared_funds"      -> 0,
            "top_up_allowance"   -> 0
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/balance")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponse.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomMetadataJson)
            .futureValue

          res.status shouldBe OK
          res.header(CORRELATION_ID).value shouldBe expectedCorrelationId
          res.json shouldBe expectedResponse
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {

        s"customer ID is invalid" in withAuthNinoRetrieval {
          val checkBalanceRequest = Json.obj(
            "epp_unique_customer_id"     -> "I am a bad customer ID.",
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(checkBalanceRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }

        s"registration ref is invalid" in withAuthNinoRetrieval {
          val checkBalanceRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> "I am a bad registration reference",
            "outbound_child_payment_ref" -> randomPaymentRef
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(checkBalanceRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }

        s"payment ref is invalid" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> "I am a bad payment reference."
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(linkRequest)
            .futureValue

          res.status shouldBe BAD_REQUEST
          val resBody = res.json.as[ErrorResponse]
          resBody shouldBe EXPECTED_JSON_ERROR_RESPONSE
        }
      }
    }

    /** Covers [[TaxFreeChildcarePaymentsController.payment()]] */
    "POST /" should {
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in withAuthNinoRetrieval {
          val expectedCorrelationId = UUID.randomUUID()
          val expectedPaymentRef    = randomPaymentRef
          val expectedPaymentDate   = randomPaymentDate

          val expectedResponse = Json.obj(
            "payment_reference"      -> expectedPaymentRef,
            "estimated_payment_date" -> expectedPaymentDate
          )

          stubFor(
            post("/individuals/tax-free-childcare/payments/")
              .withHeader(CORRELATION_ID, equalTo(expectedCorrelationId.toString))
              .willReturn(okJson(expectedResponse.toString))
          )

          val res = wsClient
            .url(s"$baseUrl/")
            .withHttpHeaders(
              AUTHORIZATION  -> "Bearer qwertyuiop",
              CORRELATION_ID -> expectedCorrelationId.toString
            )
            .post(randomPaymentRequestJson)
            .futureValue

          res.status shouldBe OK
          res.header(CORRELATION_ID).value shouldBe expectedCorrelationId
          res.json shouldBe expectedResponse
        }
      }
    }

    /** Covers `else` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
    "GET /knil" should {
      s"respond with $NOT_FOUND and a JSON ErrorResponse" in {
        val res = wsClient
          .url(s"$baseUrl/knil")
          .withHttpHeaders(
            AUTHORIZATION  -> "Bearer qwertyuiop",
            CORRELATION_ID -> UUID.randomUUID().toString
          )
          .get()
          .futureValue

        res.status shouldBe NOT_FOUND
        val resBody = res.json.as[ErrorResponse]
        resBody.statusCode shouldBe NOT_FOUND
      }
    }
  }

  private def withAuthNinoRetrieval(check: => Assertion) = {
    stubFor(
      post("/auth/authorise") willReturn okJson(Json.obj("nino" -> "QW123456A").toString)
    )

    check
  }

  private val EXPECTED_JSON_ERROR_RESPONSE = ErrorResponse(
    statusCode = BAD_REQUEST,
    message = "Provided parameters do not match expected format."
  )

  private val CORRELATION_ID = "Correlation-ID"
}
