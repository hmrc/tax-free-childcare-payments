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
import models.request.*
import models.response.NsiErrorResponse.NsiResponse
import models.response.{BalanceResponse, LinkResponse, PaymentResponse}
import utils.{ErrorResponseFactory, FormattedLogging}
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
)(using ExecutionContext)
    extends BackendController(cc)
    with FormattedLogging {

  def link(): Action[JsValue] =
    nsiAction[LinkRequest, LinkResponse](req => nsiConnector.linkAccounts(using req))

  def balance(): Action[JsValue] =
    nsiAction[SharedRequestData, BalanceResponse](req => nsiConnector.checkBalance(using req))

  def payment(): Action[JsValue] =
    nsiAction[PaymentRequest, PaymentResponse](req => nsiConnector.makePayment(using req))

  private def nsiAction[Req: Reads, Res: Writes](block: IdentifierRequest[Req] => Future[NsiResponse[Res]]) =
    identify.async(parse.json) { request =>
      given Request[JsValue] = request
      request.body.validate[Req] match {
        case JsSuccess(value, _) =>
          val requestWithValidBody: IdentifierRequest[Req] =
            IdentifierRequest(request.nino, request.correlation_id, request.map(_ => value))

          block(requestWithValidBody).map {
            case Left(nsiError)    => ErrorResponseFactory.getResult(nsiError)
            case Right(nsiSuccess) => Ok(Json.toJson(nsiSuccess))
          }
        case JsError(errors) =>
          Future.successful {
            logger.info(formattedErrorLog(errors.toString))

            BadRequest(ErrorResponseFactory.getJson(errors))
          }
      }
    }

}
