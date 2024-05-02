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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.LocalDate

class TaxFreeChildcarePaymentsControllerISpec
    extends AnyWordSpec
    with should.Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status {

  import com.github.tomakehurst.wiremock.client.WireMock._
  import models.requests.LinkResponse
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.json.Json
  import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

  import java.util.UUID
  import scala.util.Random

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
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val nsiResponse = okJson(Json.toJson(LinkResponse("", "")).toString)
          stubFor(
            post("/individuals/tax-free-childcare/payments/link") willReturn nsiResponse
          )

          val res = wsClient
            .url(s"$baseUrl/link")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(randomLinkRequestJson)
            .futureValue

          res.status shouldBe OK
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {
        s"correlationID field is not a valid UUID" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> "I am a bad UUID.",
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> randomDate
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

        s"customer ID is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> "I am a bad customer ID.",
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> randomDate
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
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> "I am a bad registration reference",
            "outbound_child_payment_ref" -> randomPaymentRef,
            "child_date_of_birth"        -> randomDate
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

        s"payment ref is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> "I am a bad payment reference.",
            "child_date_of_birth"        -> randomDate
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

        s"child DOB is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> UUID.randomUUID(),
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
      s"respond $OK" when {
        s"link request is valid, bearer token is present, auth responds with nino, and NS&I responds OK" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val nsiResponse = okJson(Json.obj(
            "correlation_id" -> UUID.randomUUID(),
            "tfc_account_status" -> "active",
            "paid_in_by_you" -> 0,
            "government_top_up" -> 0,
            "total_balance" -> 0,
            "cleared_funds" -> 0,
            "top_up_allowance" -> 0,
          ).toString)
          stubFor(
            post("/individuals/tax-free-childcare/payments/balance") willReturn nsiResponse
          )

          val res = wsClient
            .url(s"$baseUrl/balance")
            .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
            .post(randomLinkRequestJson)
            .futureValue

          res.status shouldBe OK
        }
      }

      /** Covers `if` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
      s"respond with $BAD_REQUEST and generic error message" when {
        s"correlationID field is not a valid UUID" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> "I am a bad UUID.",
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef
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

        s"customer ID is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> "I am a bad customer ID.",
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> randomPaymentRef
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
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> "I am a bad registration reference",
            "outbound_child_payment_ref" -> randomPaymentRef
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

        s"payment ref is invalid" in {
          val authResponse = okJson(Json.obj("nino" -> "QW123456A").toString)
          stubFor(
            post("/auth/authorise") willReturn authResponse
          )

          val linkRequest = Json.obj(
            "correlationId"              -> UUID.randomUUID(),
            "epp_unique_customer_id"     -> randomCustomerId,
            "epp_reg_reference"          -> randomRegistrationRef,
            "outbound_child_payment_ref" -> "I am a bad payment reference."
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

    /** Covers `else` branch of [[config.CustomJsonErrorHandler.onClientError()]]. */
    "POST /knil" should {
      s"respond with $NOT_FOUND and a JSON ErrorResponse" in {
        val res = wsClient
          .url(s"$baseUrl/knil")
          .withHttpHeaders(AUTHORIZATION -> "Bearer qwertyuiop")
          .post(randomLinkRequestJson)
          .futureValue

        res.status shouldBe NOT_FOUND
        val resBody = res.json.as[ErrorResponse]
        resBody.statusCode shouldBe NOT_FOUND
      }
    }
  }

  private def randomLinkRequestJson = Json.obj(
    "correlationId"              -> UUID.randomUUID().toString,
    "epp_unique_customer_id"     -> randomCustomerId,
    "epp_reg_reference"          -> randomRegistrationRef,
    "outbound_child_payment_ref" -> randomPaymentRef,
    "child_date_of_birth"        -> randomDate
  )

  private def randomCustomerId      = randomStringOf(EXPECTED_CUSTOMER_ID_LENGTH, '0' to '9')
  private def randomRegistrationRef = randomStringOf(EXPECTED_REGISTRATION_REF_LENGTH, ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))

  private def randomPaymentRef = {
    val letters = randomStringOf(EXPECTED_PAYMENT_REF_LETTERS, 'A' to 'Z')
    val digits  = randomStringOf(EXPECTED_PAYMENT_REF_DIGITS, '0' to '9')

    letters + digits + "TFC"
  }

  private def randomDate          = LocalDate.now() minusDays Random.nextInt(EXPECTED_MAX_CHILD_AGE_DAYS)

  private def randomStringOf(n: Int, chars: Seq[Char]) = {
    def randomChar = chars(Random.nextInt(chars.length))
    Array.fill(n)(randomChar).mkString
  }

  private val EXPECTED_CUSTOMER_ID_LENGTH      = 11
  private val EXPECTED_REGISTRATION_REF_LENGTH = 16
  private val EXPECTED_PAYMENT_REF_LETTERS     = 4
  private val EXPECTED_PAYMENT_REF_DIGITS      = 5

  private val EXPECTED_MAX_CHILD_AGE_DAYS = 18 * 365
  private val MAX_CASH_SUM_PENCE          = 100 * 1000

  private val EXPECTED_JSON_ERROR_RESPONSE = ErrorResponse(
    statusCode = BAD_REQUEST,
    message = "Provided parameters do not match expected format."
  )
}
