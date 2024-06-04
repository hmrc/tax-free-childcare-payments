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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import models.requests.IdentifierRequest
import util.FormattedLogging
import util.FormattedLogging.CORRELATION_ID

import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

@Singleton
class AuthAction @Inject() (
    val authConnector: AuthConnector,
    val parser: BodyParsers.Default
  )(implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[IdentifierRequest, AnyContent]
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions
    with FormattedLogging
    with Results
    with Status {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val req: Request[A] = request

    authorised(ConfidenceLevel.L250)
      .retrieve(Retrievals.nino) { optNino =>
        val optCorrelationIdHeader = request.headers get CORRELATION_ID
        val optIdentifierRequest   = for {
          correlationIdHeader <- optCorrelationIdHeader toRight s"Missing $CORRELATION_ID header"
          correlationId       <- Try(UUID fromString correlationIdHeader).toEither.left.map(_.getMessage)
          nino                <- optNino toRight "Unable to retrieve NI number"
        } yield IdentifierRequest(nino, correlationId, request)

        optIdentifierRequest match {
          case Right(identifierRequest) =>
            block(identifierRequest) map { result =>
              result.withHeaders(
                CORRELATION_ID -> identifierRequest.correlation_id.toString
              )
            }

          case Left(errorMessage) => Future.successful {

              logger.info(formattedLog(errorMessage))

              BadRequest(Json.toJson(
                ErrorResponse(BAD_REQUEST, errorMessage)
              ))
            }
        }
      }
  }
}
