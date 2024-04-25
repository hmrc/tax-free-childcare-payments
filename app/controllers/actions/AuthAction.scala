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

import config.AppConfig

import javax.inject.Inject
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

class AuthAction @Inject() (
    val authConnector: AuthConnector,
    val parser: BodyParsers.Default,
    config: AppConfig
  )(implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest] with AuthorisedFunctions with Logging {

  @nowarn()
  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(ConfidenceLevel.L250)
      .retrieve(Retrievals.nino) {
        case Some(nino) => block(IdentifierRequest(request, nino))
        case None       => Future.successful(BadRequest(Json.obj()))
      }
  }
}