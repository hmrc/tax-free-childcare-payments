/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.taxfreechildcarepayments.controllers

import org.mockito.Mockito.{verify, when}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

class TaxFreeChildcarePaymentsControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with OptionValues with MockitoSugar {
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  when(mockAuthConnector.authorise(any(), any[Retrieval[Option[String]]]())(any(), any()))
    .thenReturn(Future.successful(Some("nino")))
  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector)
    )
    .build()

  "POST /link" should {
    "return 200" in {
      val request = FakeRequest(
        routes.TaxFreeChildcarePaymentsController.link
      ).withBody(Json.toJson(LinkInput(someRef = "ref")))

      val result = route(app, request).value
      status(result) shouldBe Status.OK
      verify(mockAuthConnector).authorise(eqTo(ConfidenceLevel.L250), eqTo(Retrievals.nino))(any(), any())
    }
  }
}
