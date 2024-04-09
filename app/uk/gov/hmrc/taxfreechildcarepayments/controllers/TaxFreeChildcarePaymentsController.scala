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

package uk.gov.hmrc.taxfreechildcarepayments.controllers

import connectors.EisConnector
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.taxfreechildcarepayments.controllers.actions.AuthAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class EnrichedLinkInput(someRef: String, nino: String)
object EnrichedLinkInput {
  implicit lazy val format: OFormat[EnrichedLinkInput] = Json.format
}

final case class LinkInput(someRef: String)
object LinkInput {
  implicit lazy val format: OFormat[LinkInput] = Json.format
}

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (cc: ControllerComponents,
                                                    identify: AuthAction,
                                                    eisConnector: EisConnector
                                                   )(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def link(): Action[LinkInput] = identify.async(parse.json[LinkInput]) {
    implicit request =>
              val nino = request.nino
      val enrichedData = EnrichedLinkInput(request.body.someRef, nino)
      eisConnector.call(enrichedData)
        .map(ls => Ok(Json.toJson(ls)))
  }

  def balance(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("balance  is wip"))
  }

  def payment(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("payment is wip"))
  }
}
