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

import connectors.NsiConnector
import controllers.actions.AuthAction
import models.requests.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse.Maybe
import models.response.{BalanceResponse, LinkResponse, PaymentResponse, TfcErrorResponse}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) {

  def link(): Action[LinkRequest] = messageBrokerAction[LinkRequest, LinkResponse](implicit req => nsiConnector.linkAccounts)

  def balance(): Action[SharedRequestData] = messageBrokerAction[SharedRequestData, BalanceResponse](implicit req => nsiConnector.checkBalance)

  def payment(): Action[PaymentRequest] = messageBrokerAction[PaymentRequest, PaymentResponse](implicit req => nsiConnector.makePayment)

  private def messageBrokerAction[Req: Reads, Res: Writes](block: IdentifierRequest[Req] => Future[Maybe[Res]]) =
    identify.async(parse.json[Req]) { request =>
      block(request) map {
        case Left(nsiError)    => TfcErrorResponse(nsiError.reportAs, nsiError.message).toResult
        case Right(nsiSuccess) => Ok(Json.toJson(nsiSuccess))
      }
    }
}
