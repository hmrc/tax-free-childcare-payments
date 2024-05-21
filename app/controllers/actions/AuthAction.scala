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

import models.requests.IdentifierRequest
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AuthAction @Inject() (
    val authConnector: AuthConnector,
    val parser: BodyParsers.Default
  )(implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[IdentifierRequest, AnyContent] with AuthorisedFunctions with Logging with Results with Status {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(ConfidenceLevel.L250)
      .retrieve(Retrievals.nino) { optNino =>
        val optCorrelationIdHeader = request.headers get CORRELATION_ID
        val optIdentifierRequest   = for {
          correlationIdHeader <- optCorrelationIdHeader toRight s"Missing $CORRELATION_ID header."
          correlationId       <- Try(UUID fromString correlationIdHeader).toEither.left.map(_.getMessage)
          nino                <- optNino toRight "Unable to retrieve NI number."
        } yield IdentifierRequest(nino, correlationId, request)

        optIdentifierRequest match {
          case Right(identifierRequest) =>
            block(identifierRequest) map { result =>
              result.withHeaders(
                CORRELATION_ID -> identifierRequest.correlation_id.toString
              )
            }

          case Left(errorMessage) => Future.successful {
              val fullMessage = s"<${optCorrelationIdHeader.orNull}> $errorMessage"

              logger.info(fullMessage)

              BadRequest(Json.toJson(
                ErrorResponse(BAD_REQUEST, fullMessage)
              ))
            }
        }
      }
  }

  private val CORRELATION_ID = "Correlation-ID"
}
