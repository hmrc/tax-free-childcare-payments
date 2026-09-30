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
import models.request.IdentifierRequest
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import models.response.error.ErrorResponse.Response
import models.response.error.ServiceErrorResponse
import models.response.external.{ExternalBalanceResponse, ExternalLinkResponse, ExternalPaymentResponse}
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.FormattedLogging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    authAction: AuthAction,
    nsiConnector: NsiConnector
)(using ExecutionContext)
    extends BackendController(cc)
    with FormattedLogging {

  def link: Action[JsValue] =
    nsiAction[ExternalLinkRequest, ExternalLinkResponse](req => nsiConnector.linkAccounts(using req))

  def balance: Action[JsValue] =
    nsiAction[ExternalBalanceRequest, ExternalBalanceResponse](req => nsiConnector.checkBalance(using req))

  def payment: Action[JsValue] =
    nsiAction[ExternalPaymentRequest, ExternalPaymentResponse](req => nsiConnector.makePayment(using req))

  private def nsiAction[Req, Res](
      block: IdentifierRequest[Req] => Future[Response[Res]]
  )(using Reads[Req], Writes[Res]): Action[JsValue] =
    authAction.identify.async(parse.json) { request =>
      given Request[JsValue] = request
      request.body.validate[Req] match {
        case JsSuccess(value, _) =>
          val requestWithValidBody: IdentifierRequest[Req] =
            IdentifierRequest(request.nino, request.correlation_id, request.withBody(value))

          block(requestWithValidBody).map {
            case Left(nsiError)    => nsiError.toResult
            case Right(nsiSuccess) => Ok(Json.toJson(nsiSuccess))
          }
        case error: JsError =>
          Future.successful {
            logger.info(formattedErrorLog(error.errors.toString))

            ServiceErrorResponse.fromValidationError(error).toResult
          }
      }
    }

}
