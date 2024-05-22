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

import models.requests.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.{BalanceResponse, LinkResponse, PaymentResponse}
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext
  ) extends BackendHeaderCarrierProvider {
  import uk.gov.hmrc.http.HttpReads.Implicits._

  def linkAccounts(implicit req: IdentifierRequest[LinkRequest]): Future[LinkResponse] =
    httpClient
      .post(linkAccountsUrl)
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .withBody(enrichedWithNino[LinkRequest])
      .execute[LinkResponse]

  def checkBalance(implicit req: IdentifierRequest[SharedRequestData]): Future[BalanceResponse] =
    httpClient
      .post(checkBalanceUrl)
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .withBody(enrichedWithNino[SharedRequestData])
      .execute[BalanceResponse]

  def makePayment(implicit req: IdentifierRequest[PaymentRequest]): Future[PaymentResponse] =
    httpClient
      .post(makePaymentUrl)
      .setHeader(CORRELATION_ID -> req.correlation_id.toString)
      .withBody(enrichedWithNino[PaymentRequest])
      .execute[PaymentResponse]

  private def enrichedWithNino[R: OWrites](implicit req: IdentifierRequest[R]) =
    Json.toJsObject(req.body) + ("nino" -> JsString(req.nino))

  private val serviceName = "nsi"

  private def getConfig(path: String) = servicesConfig.getString(s"microservice.services.$serviceName.$path")

  private val baseUrl = {
    val domain = servicesConfig.baseUrl(serviceName)
    domain + getConfig("resourcePath")
  }

  private val linkAccountsUrl = new URL(baseUrl + getConfig("resources.link"))
  private val checkBalanceUrl = new URL(baseUrl + getConfig("resources.balance"))
  private val makePaymentUrl  = new URL(baseUrl + getConfig("resources.payment"))

  private val CORRELATION_ID = "Correlation-ID"
}
