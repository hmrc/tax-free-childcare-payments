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

import models.response.error.ServiceErrorResponse
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel, InsufficientConfidenceLevel}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveNinoAction @Inject() (
    override val authConnector: AuthConnector
)(using override val executionContext: ExecutionContext)
    extends ActionRefiner[Request, NinoRequest]
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions
    with ErrorHelper
    with Results {

  override protected[actions] def refine[A](
      request: Request[A]
  ): Future[Either[Result, NinoRequest[A]]] = {
    given RequestHeader = request

    /** Confidence level is retrieved so that it appears in implicit audit events to aid with security metrics. */
    authorised(ConfidenceLevel.L200)
      .retrieve(Retrievals.nino.and(Retrievals.confidenceLevel))
      .apply {
        case None ~ _ =>
          Future.successful(Left(logAndReturnError(ServiceErrorResponse.ETFC2 -> "Unable to retrieve NI number")))
        case Some(nino) ~ _ =>
          Future.successful(Right(NinoRequest(nino, request)))
      }
      .recover { case e: InsufficientConfidenceLevel =>
        logger.warn(
          s"${request.method} ${request.uri} failed with ${e.getClass.getName}: ${e.getMessage}",
          e
        )
        Left(
          Unauthorized(
            Json.toJson(
              uk.gov.hmrc.play.bootstrap.http.ErrorResponse(UNAUTHORIZED, e.getMessage)
            )
          )
        )
      }
  }

}
