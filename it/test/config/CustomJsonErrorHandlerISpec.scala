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

package config

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport

class CustomJsonErrorHandlerISpec
  extends AnyWordSpec
    with should.Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status
    with TableDrivenPropertyChecks {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort
    ).build()

  withClient { wsClient =>
    val contextRoot = "/individuals/tax-free-childcare/payments"
    val baseUrl     = s"http://localhost:$port$contextRoot"


  }
}
