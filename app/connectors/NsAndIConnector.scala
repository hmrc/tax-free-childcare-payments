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

import models.requests.{EnrichedLinkRequest, LinkResponse}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NsAndIConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext
  ) {

  def call(request: EnrichedLinkRequest)(implicit hc: HeaderCarrier): Future[LinkResponse] =
    httpClient
      .post(linkUrl)
      .withBody(Json.toJson(request))
      .execute[LinkResponse]

  private def getConfig(path: String) = servicesConfig.getString(s"microservice.services.ns-and-i.$path")

  private val baseUrl = {
    val domain = servicesConfig.baseUrl("ns-and-i")
    domain + getConfig("path")
  }

  private val linkUrl = new URL(baseUrl + getConfig("routes.link"))
}
