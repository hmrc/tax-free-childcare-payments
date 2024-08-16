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
import models.request.IdentifierRequest
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.JsString
import play.api.mvc.Results
import play.api.test.FakeRequest

import java.util.UUID
import scala.concurrent.Future

class AuthActionISpec extends BaseISpec with Results {
  lazy private implicit val as: ActorSystem = app.actorSystem

  private val authAction = app.injector.instanceOf[AuthAction]

  "method invokeBlock" should {
    "return a 400 Response with errorCode ETFC1 and expected errorDescription" when {
      "correlation ID is missing" in withAuthNinoRetrieval {
        val requestSansCorrelationId = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer a-totally-random-token")

        val actualResult = authAction.invokeBlock(requestSansCorrelationId, successBlock).futureValue

        checkErrorResult(actualResult, BAD_REQUEST, "ETFC1", EXPECTED_CORRELATION_ID_ERROR_DESC)
      }

      "correlation ID is invalid" in withAuthNinoRetrieval {
        val requestWithBadCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION  -> "Bearer a-totally-random-token",
          CORRELATION_ID -> "an-invalid-uuid"
        )

        val actualResult = authAction.invokeBlock(requestWithBadCorrelationId, successBlock).futureValue

        checkErrorResult(actualResult, BAD_REQUEST, "ETFC1", EXPECTED_CORRELATION_ID_ERROR_DESC)
      }
    }

    "return a 500 response with errorCode ETFC2 and expected errorDescription" when {
      "Auth doesn't return a NI number" in {
        stubFor(WireMock.post("/auth/authorise") willReturn okJson("{}"))

        val requestWithCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION  -> "Bearer a-totally-random-token",
          CORRELATION_ID -> UUID.randomUUID().toString
        )

        val actualResult = authAction.invokeBlock(requestWithCorrelationId, successBlock).futureValue

        checkErrorResult(actualResult, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_AUTH_NINO_RETRIEVAL_ERROR_DESC)
      }
    }

    lazy val successBlock = (_: IdentifierRequest[_]) => Future.successful(Ok(JsString("success")))
  }
}
