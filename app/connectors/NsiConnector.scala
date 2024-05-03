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

import models.BalanceResponse
import models.requests.{BalanceRequest, EnrichedLinkRequest, IdentifierRequest, LinkResponse}
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.http.HeaderCarrier
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

  def linkAccounts(request: EnrichedLinkRequest)(implicit hc: HeaderCarrier): Future[LinkResponse] =
    httpClient
      .post(linkAccountsUrl)
      .withBody(Json.toJson(request))
      .execute[LinkResponse]

  def checkBalance(implicit req: IdentifierRequest[BalanceRequest]): Future[BalanceResponse] =
    httpClient
      .post(checkBalanceUrl)
      .withBody(Json.toJsObject(req.body) + ("nino", JsString(req.nino)))
      .execute[BalanceResponse]

  private val serviceName = "nsi"

  private def getConfig(path: String) = servicesConfig.getString(s"microservice.services.$serviceName.$path")

  private val baseUrl = {
    val domain = servicesConfig.baseUrl(serviceName)
    domain + getConfig("resourcePath")
  }

  private val linkAccountsUrl = new URL(baseUrl + getConfig("resources.link"))
  private val checkBalanceUrl = new URL(baseUrl + getConfig("resources.balance"))
}
