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

package uk.gov.hmrc.taxfreechildcarepayments.controllers

import connectors.{EisConnector, LinkResponse}

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
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
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}

class TaxFreeChildcarePaymentsControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with OptionValues with MockitoSugar {
  val mockAuthConnector: AuthConnector        = mock[AuthConnector]
  when(mockAuthConnector.authorise(any(), any[Retrieval[Option[String]]]())(any(), any()))
    .thenReturn(Future.successful(Some("nino")))

  val mockEisConnector: EisConnector = mock[EisConnector]
  when(mockEisConnector.call(any())(any()))
    .thenReturn(Future.successful(LinkResponse("success")))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[EisConnector].toInstance(mockEisConnector)
    )
    .build()

  "POST /link" should {
    "return 200" in {
      val request = FakeRequest(
        routes.TaxFreeChildcarePaymentsController.link
      ).withBody(Json.toJson(LinkInput("ref")))

      val result = route(app, request).value
      status(result) shouldBe Status.OK
      verify(mockAuthConnector).authorise(eqTo(ConfidenceLevel.L250), eqTo(Retrievals.nino))(any(), any())
    }
  }
}
