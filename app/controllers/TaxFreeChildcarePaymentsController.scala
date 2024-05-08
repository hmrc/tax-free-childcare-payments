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
import models.requests.{LinkRequest, PaymentRequest, RequestMetadata}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class TaxFreeChildcarePaymentsController @Inject() (
    cc: ControllerComponents,
    identify: AuthAction,
    nsiConnector: NsiConnector
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc) {

  def link(): Action[LinkRequest] = identify.async(parse.json[LinkRequest]) { implicit req =>
    nsiConnector
      .linkAccounts
      .map {
        okJson(req.body.metadata.correlation_id, _)
      }
  }

  def balance(): Action[RequestMetadata] = identify.async(parse.json[RequestMetadata]) { implicit req =>
    nsiConnector
      .checkBalance
      .map {
        okJson(req.body.correlation_id, _)
      }
  }

  def payment(): Action[PaymentRequest] = identify.async(parse.json[PaymentRequest]) { implicit req =>
    nsiConnector
      .makePayment
      .map {
        okJson(req.body.metadata.correlation_id, _)
      }
  }

  private def okJson[Res: OWrites](correlation_id: UUID, nsiResponse: Res) =
    Ok(Json.toJsObject(nsiResponse) + ("correlation_id" -> Json.toJson(correlation_id)))
}
