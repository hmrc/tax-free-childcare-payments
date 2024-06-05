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

package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import connectors.NsiConnector
import controllers.actions.AuthAction
import models.requests.{IdentifierRequest, PaymentRequest, SharedRequestData}
import models.response.{BalanceResponse, PaymentResponse}

import play.api.libs.json.{Json, OWrites, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) {

//  def link(): Action[LinkRequest] = messageBrokerAction[LinkRequest, LinkResponse](implicit req => nsiConnector.linkAccounts)
  def link(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("Hello from TFCP API"))
  }

  def balance(): Action[SharedRequestData] = messageBrokerAction[SharedRequestData, BalanceResponse](implicit req => nsiConnector.checkBalance)

  def payment(): Action[PaymentRequest] = messageBrokerAction[PaymentRequest, PaymentResponse](implicit req => nsiConnector.makePayment)

  private def messageBrokerAction[Req: Reads, Res: OWrites](block: IdentifierRequest[Req] => Future[Res]) =
    identify.async(parse.json[Req]) { request =>
      block(request) map { response =>
        Ok(Json.toJson(response))
      }
    }
}
