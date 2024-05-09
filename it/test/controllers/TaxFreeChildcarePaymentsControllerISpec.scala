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
import play.api.libs.json.JsValue
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

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

  import java.util.UUID

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
          val expectedResponseJson = Json.obj(
            "correlation_id"  -> UUID.randomUUID(),
            "child_full_name" -> "Peter Pan"
          )

          val correlation_id   = (expectedResponseJson \ "correlation_id").as[JsValue]
          val submittedPayload = randomLinkRequestJson + ("correlation_id" -> correlation_id)
          val nsiResponseBody  = expectedResponseJson - "correlation_id"

          stubFor(
            post("/individuals/tax-free-childcare/payments/link") willReturn okJson(nsiResponseBody.toString)
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(submittedPayload)
            .futureValue

          res.status shouldBe OK
          res.json shouldBe expectedResponseJson
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {

        s"correlationID field is not a valid UUID" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "correlation_id"             -> "I am a bad UUID.",
            "epp_unique_customer_id"     -> randomCustomerId,
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

        s"customer ID is invalid" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"             -> UUID.randomUUID(),
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
          val expectedResponse = Json.obj(
            "correlation_id"     -> UUID.randomUUID(),
            "tfc_account_status" -> "active",
            "paid_in_by_you"     -> 0,
            "government_top_up"  -> 0,
            "total_balance"      -> 0,
            "cleared_funds"      -> 0,
            "top_up_allowance"   -> 0
          )
          val nsiResponse      = expectedResponse - "correlation_id"
          stubFor(
            post("/individuals/tax-free-childcare/payments/balance") willReturn okJson(nsiResponse.toString)
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(randomMetadataJsonWith((expectedResponse \ "correlation_id").as[UUID]))
            .futureValue

          res.status shouldBe OK
          res.json shouldBe expectedResponse
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {

        s"correlationID field is not a valid UUID" in withAuthNinoRetrieval {
          val linkRequest = Json.obj(
            "correlation_id"             -> "I am a bad UUID.",
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef
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

        s"customer ID is invalid" in withAuthNinoRetrieval {
          val checkBalanceRequest = Json.obj(
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"             -> UUID.randomUUID(),
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
            "correlation_id"         -> expectedCorrelationId,
            "payment_reference"      -> expectedPaymentRef,
            "estimated_payment_date" -> expectedPaymentDate
          )

          val nsiResponse = expectedResponse - "correlation_id"
          stubFor(
            post("/individuals/tax-free-childcare/payments/") willReturn okJson(nsiResponse.toString)
          )

          val res = wsClient
            .url(s"$baseUrl/")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(randomPaymentRequestJson(expectedCorrelationId))
            .futureValue

          res.status shouldBe OK
          res.json shouldBe expectedResponse
        }
      }
    }

    /** Covers `else` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
    "GET /knil" should {
      s"respond with $NOT_FOUND and a JSON ErrorResponse" in {
        val res = wsClient
          .url(s"$baseUrl/knil")
          .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
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

  private val EXPECTED_JSON_ERROR_RESPONSE               = ErrorResponse(
    statusCode = BAD_REQUEST,
    message = "Provided parameters do not match expected format."
  )
}
