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

package uk.gov.hmrc.taxfreechildcarepayments.connectors

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import models.requests.{EnrichedLinkRequest, LinkResponse}

import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

@Singleton
class NsNiConnector @Inject() (
    httpClient: HttpClientV2,
    configuration: Configuration
  )(implicit ec: ExecutionContext
  ) {

  def call(request: EnrichedLinkRequest)(implicit hc: HeaderCarrier): Future[LinkResponse] =
    httpClient
      .post(new URI("http://localhost:10501/individuals/tax-free-childcare/payments/link").toURL)
      .withBody(Json.toJson(request))
      .execute[LinkResponse]
}
