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
import helpers.generators.IdentifierRequestGenerators
import models.request.IdentifierRequest
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Results.Ok
import utils.FormattedLogging.CORRELATION_ID

import scala.concurrent.{ExecutionContext, Future}

class AddResponseCorrelationIdHeaderActionSpec extends BaseSpec with IdentifierRequestGenerators with ScalaFutures {

  private val genUnitIdentifierRequests: Gen[IdentifierRequest[Unit]] = genIdentifierRequests(Gen.const(()))

  private val executionContext: ExecutionContext = ExecutionContext.global

  private val action: AddResponseCorrelationIdHeaderAction = new AddResponseCorrelationIdHeaderAction()(
    using executionContext
  )

  "add correlation id header action" should {
    "copy the correlation id from the request to the response" in
      forAll(genUnitIdentifierRequests) { request =>

        val result = action.invokeBlock[Unit](request, _ => Future.successful(Ok)).futureValue

        result shouldBe Ok.withHeaders(CORRELATION_ID -> request.correlation_id.toString)

      }
  }

}
