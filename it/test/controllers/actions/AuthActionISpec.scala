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

package controllers.actions

import base.BaseISpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{okJson, stubFor}
import models.requests.IdentifierRequest
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.JsString
import play.api.mvc.{AnyContentAsEmpty, Results}
import play.api.test.FakeRequest

import java.util.UUID
import scala.concurrent.Future

class AuthActionISpec extends BaseISpec with Results {
  private implicit val as: ActorSystem = app.actorSystem

  private val authAction = app.injector.instanceOf[AuthAction]

  "method invokeBlock" should {
    "return a 400 Response with errorCode ETFC1 and expected errorDescription" when {
      "correlation ID is missing" in withAuthNinoRetrieval {
        val requestSansCorrelationId = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer a-totally-random-token")

        val actualResult = authAction.invokeBlock(requestSansCorrelationId, successBlock[AnyContentAsEmpty.type]).futureValue

        checkErrorResult(actualResult, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION)
      }

      "correlation ID is invalid" in withAuthNinoRetrieval {
        val requestSansCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION -> "Bearer a-totally-random-token",
          CORRELATION_ID -> "an-invalid-uuid"
        )

        val actualResult = authAction.invokeBlock(requestSansCorrelationId, successBlock[AnyContentAsEmpty.type]).futureValue

        checkErrorResult(actualResult, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION)
      }
    }

    "return a 500 response with errorCode ETFC1 and expected errorDescription" when {
      "Auth doesn't return a NI number" in {
        stubFor(WireMock.post("/auth/authorise") willReturn okJson("{}"))

        val requestSansCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION -> "Bearer a-totally-random-token",
          CORRELATION_ID -> UUID.randomUUID().toString
        )

        val actualResult = authAction.invokeBlock(requestSansCorrelationId, successBlock[AnyContentAsEmpty.type]).futureValue

        checkErrorResult(actualResult, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_500_ERROR_DESCRIPTION)
      }
    }

    def successBlock[A](req: IdentifierRequest[A]) = Future.successful(Ok(JsString("success")))
  }
}
