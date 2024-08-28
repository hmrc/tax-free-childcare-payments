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
import models.request._
import models.response.NsiErrorResponse.Maybe
import models.response.{BalanceResponse, LinkResponse, PaymentResponse}
import utils.{ErrorResponseFactory, FormattedLogging}

import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
  )(implicit
    ec: ExecutionContext,
    readsPayee: Reads[Payee]
  ) extends BackendController(cc) with FormattedLogging {
  import TaxFreeChildcarePaymentsController._

  def link(): Action[JsValue] = nsiAction[LinkRequest, LinkResponse](implicit req => nsiConnector.linkAccounts)

  def balance(): Action[JsValue] = nsiAction[SharedRequestData, BalanceResponse](implicit req => nsiConnector.checkBalance)

  def payment(): Action[JsValue] = nsiAction[PaymentRequest, PaymentResponse](implicit req => nsiConnector.makePayment)

  private def nsiAction[Req: Reads, Res: Writes](block: IdentifierRequest[Req] => Future[Maybe[Res]]) =
    identify.async(parse.json) { implicit request =>
      request.body.validate[Req] match {
        case JsSuccess(value, _) =>
          val requestWithValidBody = IdentifierRequest(request.nino, request.correlation_id, request.map(_ => value))

          block(requestWithValidBody) map {
            case Left(nsiError)    => ErrorResponseFactory getResult nsiError
            case Right(nsiSuccess) => Ok(Json.toJson(nsiSuccess))
          }
        case JsError(errors)     => Future.successful {
            logger.info(formattedLog(errors.toString))

            BadRequest(ErrorResponseFactory getJson errors)
          }
      }
    }
}

object TaxFreeChildcarePaymentsController {

  private implicit val writesBalanceResponse: Writes[BalanceResponse] = br =>
    Json.obj(
      "tfc_account_status" -> br.accountStatus,
      "government_top_up"  -> br.topUpAvailable,
      "top_up_allowance"   -> br.topUpRemaining,
      "paid_in_by_you"     -> br.paidIn,
      "total_balance"      -> br.totalBalance,
      "cleared_funds"      -> br.clearedFunds
    )
}
