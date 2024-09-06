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

import models.request.IdentifierRequest
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.{ErrorResponseFactory, FormattedLogging}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AuthAction @Inject() (
    val authConnector: AuthConnector,
    val parser: BodyParsers.Default
  )(implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[IdentifierRequest, AnyContent]
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions
    with FormattedLogging
    with Results {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val req: Request[A] = request

    authorised(ConfidenceLevel.L250 and AffinityGroup.Individual)
      .retrieve(Retrievals.nino) {
        optNino =>
          val optCorrelationIdHeader = request.headers get CORRELATION_ID
          val optIdentifierRequest   = for {
            correlationIdHeader <- optCorrelationIdHeader toRight ETFC1                            -> "Correlation-ID header is missing"
            correlationId       <- Try(UUID fromString correlationIdHeader).toOption toRight ETFC1 -> "Correlation-ID header is invalid"
            nino                <- optNino toRight ETFC2                                           -> "Unable to retrieve NI number"
          } yield IdentifierRequest(nino, correlationId, request)

          optIdentifierRequest match {
            case Right(identifierRequest) =>
              block(identifierRequest) map { result =>
                result.withHeaders(
                  CORRELATION_ID -> identifierRequest.correlation_id.toString
                )
              }

            case Left((errorResponse, logMessage)) => Future.successful {
                logger.info(formattedLog(logMessage))

                errorResponse
              }
          }
      }
  }

  private lazy val ETFC1 = BadRequest(ErrorResponseFactory.getJson("ETFC1", "Correlation ID is in an invalid format or is missing"))
  private lazy val ETFC2 = InternalServerError(ErrorResponseFactory.getJson("ETFC2", "Bearer Token did not return a valid record"))
}
