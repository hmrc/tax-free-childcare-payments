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

package controllers

import connectors.NsiConnector
import helpers.BaseSpec
import helpers.auth.AuthMocks
import helpers.generators.request.external.{
  ExternalBalanceRequestGenerators,
  ExternalLinkRequestGenerators,
  ExternalPaymentRequestGenerators
}
import helpers.generators.response.external.{
  ExternalBalanceResponseGenerators,
  ExternalLinkResponseGenerators,
  ExternalPaymentResponseGenerators
}
import helpers.json.TestFormats
import models.response.error.NsiErrorResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Unauthorized}
import play.api.test.FakeRequest

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TaxFreeChildcarePaymentControllerSpec
    extends BaseSpec
    with MockitoSugar
    with BeforeAndAfterEach
    with AuthMocks
    with ScalaFutures
    with ExternalLinkRequestGenerators
    with ExternalLinkResponseGenerators
    with ExternalBalanceRequestGenerators
    with ExternalBalanceResponseGenerators
    with ExternalPaymentRequestGenerators
    with ExternalPaymentResponseGenerators
    with TestFormats {

  private val nsiConnector: NsiConnector = mock[NsiConnector]

  private val executionContext: ExecutionContext = cc.executionContext

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(nsiConnector)
  }

  private val controller = TaxFreeChildcarePaymentsController(
    cc = cc,
    nsiConnector = nsiConnector,
    authAction = authAction
  )(using executionContext)

  private val nino          = "nino"
  private val correlationId = UUID.randomUUID()

  private val externalLinkRequestJson = genExternalLinkRequestJsObjects.sample.get
  private val externalLinkResponse    = genExternalLinkResponses.sample.get

  private val linkRequest: FakeRequest[JsValue] =
    FakeRequest("POST", "/link").withBody(externalLinkRequestJson)

  private val externalBalanceRequestJson = genExternalBalanceRequestJsObjects.sample.get
  private val externalBalanceResponse    = genExternalBalanceResponses.sample.get

  private val balanceRequest: FakeRequest[JsValue] =
    FakeRequest("POST", "/balance").withBody(externalBalanceRequestJson)

  private val externalPaymentRequestJson = genExternalPaymentRequestJsObjects.sample.get
  private val externalPaymentResponse    = genExternalPaymentResponses.sample.get

  private val paymentRequest: FakeRequest[JsValue] =
    FakeRequest("POST", "/payment").withBody(externalPaymentRequestJson)

  "link" should {
    "call authActions.identify" in {
      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.linkAccounts(using any()))
        .thenReturn(Future.successful(Right(externalLinkResponse)))

      controller.link(linkRequest).futureValue

      verify(authAction).identify
    }

    "early return if auth fails, without calling nsiConnector" in {
      mockAuthFailure()

      val result: Result = controller.link(linkRequest).futureValue

      result shouldBe Unauthorized

      verifyNoInteractions(nsiConnector)
    }

    "call nsiConnector.linkAccounts" in {
      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.linkAccounts(using any()))
        .thenReturn(Future.successful(Right(externalLinkResponse)))

      controller.link(linkRequest).futureValue

      verify(nsiConnector).linkAccounts(using any())
    }

    "return 200" when {
      "nsiConnector.linkAccounts returns Right" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.linkAccounts(using any()))
          .thenReturn(Future.successful(Right(externalLinkResponse)))

        val result = controller.link(linkRequest).futureValue

        result shouldBe Ok(Json.toJson(externalLinkResponse))
      }
    }

    "return error" when {
      "nsiConnector.linkAccounts returns Left" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.linkAccounts(using any()))
          .thenReturn(Future.successful(Left(NsiErrorResponse.E0034)))

        val result = controller.link(linkRequest).futureValue

        result shouldBe NsiErrorResponse.E0034.toResult
      }
    }

  }

  "balance" should {
    "call authActions.identify" in {
      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.checkBalance(using any()))
        .thenReturn(Future.successful(Right(externalBalanceResponse)))

      controller.balance(balanceRequest).futureValue

      verify(authAction).identify
    }

    "early return if auth fails, without calling nsiConnector" in {
      mockAuthFailure()

      val result: Result = controller.balance(balanceRequest).futureValue

      result shouldBe Unauthorized

      verifyNoInteractions(nsiConnector)
    }

    "call nsiConnector.checkBalance" in {

      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.checkBalance(using any()))
        .thenReturn(Future.successful(Right(externalBalanceResponse)))

      controller.balance(balanceRequest).futureValue

      verify(nsiConnector).checkBalance(using any())
    }

    "return 200" when {
      "nsiConnector.checkBalance returns Right" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.checkBalance(using any()))
          .thenReturn(Future.successful(Right(externalBalanceResponse)))

        val result = controller.balance(balanceRequest).futureValue

        result shouldBe Ok(Json.toJson(externalBalanceResponse))
      }
    }

    "return error" when {
      "nsiConnector.checkBalance returns Left" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.checkBalance(using any()))
          .thenReturn(Future.successful(Left(NsiErrorResponse.E0034)))

        val result = controller.balance(balanceRequest).futureValue

        result shouldBe NsiErrorResponse.E0034.toResult
      }
    }

  }

  "payment" should {
    "call authActions.identify" in {
      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.makePayment(using any()))
        .thenReturn(Future.successful(Right(externalPaymentResponse)))

      controller.payment(paymentRequest).futureValue

      verify(authAction).identify
    }

    "early return if auth fails, without calling nsiConnector" in {
      mockAuthFailure()

      val result: Result = controller.payment(paymentRequest).futureValue

      result shouldBe Unauthorized

      verifyNoInteractions(nsiConnector)
    }

    "call nsiConnector.makePayment" in {
      mockAuthSuccess(nino, correlationId)
      when(nsiConnector.makePayment(using any()))
        .thenReturn(Future.successful(Right(externalPaymentResponse)))

      controller.payment(paymentRequest).futureValue

      verify(nsiConnector).makePayment(using any())
    }

    "return 200" when {
      "nsiConnector.makePayment returns Right" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.makePayment(using any()))
          .thenReturn(Future.successful(Right(externalPaymentResponse)))

        val result = controller.payment(paymentRequest).futureValue

        result shouldBe Ok(Json.toJson(externalPaymentResponse))
      }
    }

    "return error" when {
      "nsiConnector.makePayment returns Left" in {
        mockAuthSuccess(nino, correlationId)
        when(nsiConnector.makePayment(using any()))
          .thenReturn(Future.successful(Left(NsiErrorResponse.E0034)))

        val result = controller.payment(paymentRequest).futureValue

        result shouldBe NsiErrorResponse.E0034.toResult
      }
    }

  }

}
