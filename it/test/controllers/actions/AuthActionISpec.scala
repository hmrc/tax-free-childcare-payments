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
import helpers.AuthStubs
import models.request.IdentifierRequest
import models.request.data.Generators
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest

import java.util.UUID
import scala.concurrent.Future

class AuthActionISpec extends BaseISpec with Results with AuthStubs with Generators{
  given ActorSystem = app.actorSystem

  private val authActions = app.injector.instanceOf[AuthAction].identify

  val successBlock: IdentifierRequest[?] => Future[Result] = (_: IdentifierRequest[?]) =>
    Future.successful(Ok(JsString("success")))

  "auth actions" should {
    "return a 400 Response with errorCode ETFC1 and expected errorDescription" when {
      "correlation ID is missing" in {
        stubAuthRetrievalOf(randomNinos.sample.get)

        val requestSansCorrelationId = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer a-totally-random-token")

        val actualResult = authActions.invokeBlock(requestSansCorrelationId, successBlock).futureValue

        val resultJson = Json.parse(actualResult.body.consumeData.futureValue.toArray)

        (actualResult.header.status, resultJson) shouldBe (
          BAD_REQUEST,
          Json.obj("errorCode" -> "ETFC1", "errorDescription" -> expectedCorrelationIdErrorDesc)
        )
      }

      "correlation ID is invalid" in {
        stubAuthRetrievalOf(randomNinos.sample.get)

        val requestWithBadCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION  -> "Bearer a-totally-random-token",
          CORRELATION_ID -> "an-invalid-uuid"
        )

        val actualResult = authActions.invokeBlock(requestWithBadCorrelationId, successBlock).futureValue

        val resultJson = Json.parse(actualResult.body.consumeData.futureValue.toArray)

        (actualResult.header.status, resultJson) shouldBe (
          BAD_REQUEST,
          Json.obj("errorCode" -> "ETFC1", "errorDescription" -> expectedCorrelationIdErrorDesc)
        )
      }
    }

    "return a 500 response with errorCode ETFC2 and expected errorDescription" when {
      "Auth doesn't return a NI number" in {
        stubAuthEmptyRetrieval

        val requestWithCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION  -> "Bearer a-totally-random-token",
          CORRELATION_ID -> UUID.randomUUID().toString
        )

        val actualResult = authActions.invokeBlock(requestWithCorrelationId, successBlock).futureValue

        val resultJson = Json.parse(actualResult.body.consumeData.futureValue.toArray)

        (actualResult.header.status, resultJson) shouldBe (
          INTERNAL_SERVER_ERROR,
          Json.obj("errorCode" -> "ETFC2", "errorDescription" -> expectedAuthNinoRetrievalErrorDesc)
        )
      }
    }

    "return a 401 response and expected errorDescription" when {
      "confidence level is insufficient" in {
        stubAuthWithLowConfidenceLevel

        val requestWithCorrelationId = FakeRequest().withHeaders(
          AUTHORIZATION  -> "Bearer a-totally-random-token",
          CORRELATION_ID -> UUID.randomUUID().toString
        )

        val actualResult = authActions.invokeBlock(requestWithCorrelationId, successBlock).futureValue
        val responseBody = actualResult.body.consumeData.futureValue.toArray
        val responseJson = Json.parse(responseBody)

        actualResult.header.status shouldBe UNAUTHORIZED
        (responseJson \ "statusCode").as[Int] shouldBe UNAUTHORIZED
        (responseJson \ "message").as[String] shouldBe expectedConfidenceLevelErrorDesc
      }
    }

    "add the correlation id header from the request onto the response" in {
      stubAuthRetrievalOf("nino")

      val correlationId = UUID.randomUUID.toString

      val requestWithCorrelationId =
        FakeRequest().withHeaders(AUTHORIZATION -> "Bearer a-totally-random-token", CORRELATION_ID -> correlationId)

      val actualResult = authActions.invokeBlock(requestWithCorrelationId, successBlock).futureValue

      actualResult.header.headers(CORRELATION_ID) shouldBe correlationId
    }
  }

}
