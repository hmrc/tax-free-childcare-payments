/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import models.requests.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse.Maybe
import models.response.{BalanceResponse, LinkResponse, PaymentResponse}

import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext
  ) extends BackendHeaderCarrierProvider {
  import uk.gov.hmrc.http.HttpReads.Implicits._

  private implicit def httpReadsEither[A: Reads, B: Reads]: HttpReads[Either[B, A]] =
    HttpReads[HttpResponse].map { response =>
      response.json.validate[A].asOpt toRight response.json.as[B]
    }

  def linkAccounts(implicit req: IdentifierRequest[LinkRequest]): Future[Maybe[LinkResponse]] =
    httpClient
      .post(resource(req.body.metadata.outbound_child_payment_ref, "linkAccounts"))
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .withBody(enrichedWithNino[LinkRequest])
      .execute[Maybe[LinkResponse]]

  def checkBalance(implicit req: IdentifierRequest[SharedRequestData]): Future[Maybe[BalanceResponse]] =
    httpClient
      .get(resource(req.body.outbound_child_payment_ref, "balance"))
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .execute[Maybe[BalanceResponse]]

  def makePayment(implicit req: IdentifierRequest[PaymentRequest]): Future[Maybe[PaymentResponse]] =
    httpClient
      .post(resource(req.body.metadata.outbound_child_payment_ref, "payments"))
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .withBody(enrichedWithNino[PaymentRequest])
      .execute[Maybe[PaymentResponse]]

  private def enrichedWithNino[R: OWrites](implicit req: IdentifierRequest[R]) =
    Json.toJsObject(req.body) + ("nino" -> JsString(req.nino))

  private val serviceName = "nsi"

  private def resource(accountRef: String, endpoint: String) = {
    val domain       = servicesConfig.baseUrl(serviceName)
    val rootPath     = servicesConfig.getString(s"microservice.services.$serviceName.path")
    val resourcePath = servicesConfig.getString(s"microservice.services.$serviceName.$endpoint")

    new URL(s"$domain$rootPath$resourcePath/$accountRef")
  }

  private val CORRELATION_ID = "Correlation-ID"

  implicit val readsLinkResponse: Reads[LinkResponse] =
    (__ \ "childFullName").read[String] map LinkResponse.apply
}
