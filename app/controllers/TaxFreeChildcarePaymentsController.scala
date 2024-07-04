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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import connectors.NsiConnector
import controllers.actions.AuthAction
import models.requests.Payee.ChildCareProvider
import models.requests._
import models.response.NsiErrorResponse.Maybe
import models.response.{BalanceResponse, LinkResponse, PaymentResponse, TfcErrorResponse}

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) {
  import TaxFreeChildcarePaymentsController._

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

object TaxFreeChildcarePaymentsController extends ConstraintReads {

  private implicit val readsLinkReq: Reads[LinkRequest] = (
    __.read[SharedRequestData] ~
      (__ \ "child_date_of_birth").read[LocalDate]
  )(LinkRequest.apply _)

  private implicit val readsPaymentReq: Reads[PaymentRequest] = (
    __.read[SharedRequestData] ~
      (__ \ "payment_amount").read[Int] ~
      of[Payee]
  )(PaymentRequest.apply _)

  lazy private implicit val readsPayee: Reads[Payee] = (
    (__ \ "payee_type").readNullable[String](verifying(_ equalsIgnoreCase "ccp")) ~
      of[ChildCareProvider]
  )((_, ccp) => ccp)

  lazy private implicit val readsCcp: Reads[ChildCareProvider] = (
    (__ \ "ccp_reg_reference").read[String] ~
      (__ \ "ccp_postcode").read[String]
  )(ChildCareProvider.apply _)

  lazy private implicit val readsSharedRequestData: Reads[SharedRequestData] = (
    (__ \ "epp_unique_customer_id").read(NON_EMPTY_ALPHA_NUM_STR_PATTERN) ~
      (__ \ "epp_reg_reference").read(NON_EMPTY_ALPHA_NUM_STR_PATTERN) ~
      (__ \ "outbound_child_payment_ref").read(NON_EMPTY_ALPHA_NUM_STR_PATTERN)
  )(SharedRequestData.apply _)

  lazy private val NON_EMPTY_ALPHA_NUM_STR_PATTERN = pattern("[a-zA-Z0-9]+".r)

  private implicit val writesLinkResponse: Writes[LinkResponse] = lr =>
    Json.obj(
      "child_full_name" -> lr.childFullName
    )

  private implicit val writesBalanceResponse: Writes[BalanceResponse] = br =>
    Json.obj(
      "tfc_account_status" -> br.accountStatus,
      "government_top_up"  -> br.topUpAvailable,
      "top_up_allowance"   -> br.topUpRemaining,
      "paid_in_by_you"     -> br.paidIn,
      "total_balance"      -> br.totalBalance,
      "cleared_funds"      -> br.clearedFunds
    )

  private implicit val writesPaymentResponse: Writes[PaymentResponse] = pr =>
    Json.obj(
      "payment_reference"      -> pr.payment_reference,
      "estimated_payment_date" -> pr.estimated_payment_date
    )
}
