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

package controllers.actions

import base.BaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel}

import scala.concurrent.{ExecutionContext, Future}

class RetrieveNinoActionSpec extends BaseSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  private val authConnector: AuthConnector = mock[AuthConnector]

  private val executionContext: ExecutionContext = ExecutionContext.global

  private val action = new RetrieveNinoAction(authConnector)(using executionContext)

  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)
  }

  private def mockRetrieval(result: Future[Option[String] ~ ConfidenceLevel]) =
    when(authConnector.authorise[Option[String] ~ ConfidenceLevel](any(), any())(using any(), any()))
      .thenReturn(result)

  "retrieve nino action" should {
    "return IdentifierRequest with relevant nino and correlation id" when {
      "auth is successful" in {
        mockRetrieval(Future.successful(new ~(Some("nino"), ConfidenceLevel.L200)))

        val result: Either[Result, NinoRequest[AnyContentAsEmpty.type]] = action.refine(fakeRequest).futureValue

        result shouldBe Right(NinoRequest("nino", fakeRequest))
      }
    }

    "return 500 with ETFC2" when {
      "auth retrieval brings back no nino" in {
        mockRetrieval(Future.successful(new ~(None, ConfidenceLevel.L200)))

        val result: Either[Result, NinoRequest[AnyContentAsEmpty.type]] = action.refine(fakeRequest).futureValue

        result shouldBe Left(
          InternalServerError(
            Json.obj("errorCode" -> "ETFC2", "errorDescription" -> "Bearer Token did not return a valid record")
          )
        )
      }
    }

    "return 401" when {
      "the confidence level is insufficient" in {
        mockRetrieval(Future.failed(InsufficientConfidenceLevel()))

        val result: Either[Result, NinoRequest[AnyContentAsEmpty.type]] = action.refine(fakeRequest).futureValue

        result shouldBe Left(
          Unauthorized(
            Json.obj("statusCode" -> 401, "message" -> "Insufficient ConfidenceLevel")
          )
        )
      }
    }
  }

}
