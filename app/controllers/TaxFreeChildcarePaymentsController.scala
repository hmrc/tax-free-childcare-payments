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
import models.requests.{EnrichedLinkRequest, LinkRequest}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
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

  def link(): Action[LinkRequest] = identify.async(parse.json[LinkRequest]) {
    implicit request =>
      val enrichedData = EnrichedLinkRequest(
        request.body.correlationId.toString,
        request.body.epp_unique_customer_id,
        request.body.epp_reg_reference,
        request.body.outbound_child_payment_ref,
        request.body.child_date_of_birth.toString,
        request.nino
      )
      nsiConnector.call(enrichedData)
        .map(ls => Ok(Json.toJson(ls)))
  }

  def balance(): Action[AnyContent] = Action.async {
    Future.successful(Ok("balance  is wip"))
  }

  def payment(): Action[AnyContent] = Action.async {
    Future.successful(Ok("payment is wip"))
  }
}
