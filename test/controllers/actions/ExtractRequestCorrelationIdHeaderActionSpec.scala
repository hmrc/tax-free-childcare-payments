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

import helpers.BaseSpec
import models.request.IdentifierRequest
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import utils.FormattedLogging.CORRELATION_ID

import java.util.UUID
import scala.concurrent.ExecutionContext

class ExtractRequestCorrelationIdHeaderActionSpec extends BaseSpec with ScalaFutures {

  val executionContext: ExecutionContext = ExecutionContext.global

  val action = new ExtractRequestCorrelationIdHeaderAction()(using executionContext)

  "extract request correlation_id header action" should {
    "successfully extract the correlation_id header" when {
      "the correlation_id header is present and in correct format" in {

        val uuid = UUID.randomUUID()

        val request = NinoRequest("nino", FakeRequest().withHeaders(CORRELATION_ID -> uuid.toString))

        action.refine(request).futureValue shouldBe Right(IdentifierRequest("nino", uuid, request))
      }
    }

    "return 400 with ETFC1" when {
      "the correlation_id header is missing" in {
        val request = NinoRequest("nino", FakeRequest())

        action.refine(request).futureValue shouldBe Left(
          BadRequest(
            Json.obj(
              "errorCode"        -> "ETFC1",
              "errorDescription" -> "Correlation ID is in an invalid format or is missing"
            )
          )
        )

      }

      "the correlation_id header is present, but in wrong format" in {
        val request = NinoRequest("nino", FakeRequest().withHeaders(CORRELATION_ID -> "invalid-uuid"))

        action.refine(request).futureValue shouldBe Left(
          BadRequest(
            Json.obj(
              "errorCode"        -> "ETFC1",
              "errorDescription" -> "Correlation ID is in an invalid format or is missing"
            )
          )
        )

      }
    }
  }

}
