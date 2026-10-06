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

import models.request.IdentifierRequest
import play.api.mvc.{ActionFunction, Result}
import utils.FormattedLogging.CORRELATION_ID

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddResponseCorrelationIdHeaderAction @Inject() ()(using override val executionContext: ExecutionContext)
    extends ActionFunction[IdentifierRequest, IdentifierRequest] {

  override def invokeBlock[A](
      request: IdentifierRequest[A],
      block: IdentifierRequest[A] => Future[Result]
  ): Future[Result] =
    block(request).map { result =>
      result.withHeaders(
        CORRELATION_ID -> request.correlation_id.toString
      )
    }

}
