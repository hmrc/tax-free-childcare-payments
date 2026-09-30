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
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, RequestHeader, Result}
import utils.FormattedLogging.CORRELATION_ID
import utils.{ErrorResponseFactory, FormattedLogging}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ExtractRequestCorrelationIdHeaderAction @Inject() ()(using override val executionContext: ExecutionContext)
    extends ActionRefiner[NinoRequest, IdentifierRequest]
    with FormattedLogging {

  override protected[actions] def refine[A](request: NinoRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    given RequestHeader = request

    val either: Either[(Result, String), IdentifierRequest[A]] = for {
      correlationIdHeader <- request.headers
        .get(CORRELATION_ID)
        .toRight(
          ETFC1 -> "Correlation-ID header is missing"
        )
      correlationId <- Try(UUID.fromString(correlationIdHeader)).toOption
        .toRight(ETFC1 -> "Correlation-ID header is invalid")
    } yield IdentifierRequest(request.nino, correlationId, request)

    Future.successful(either.left.map { case (result, errorMessage) =>
      logger.info(formattedErrorLog(errorMessage))

      result
    })
  }

  private val ETFC1 = BadRequest(
    ErrorResponseFactory.getJson("ETFC1", "Correlation ID is in an invalid format or is missing")
  )

}
