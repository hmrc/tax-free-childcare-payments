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

package base

import org.scalatest.Assertion
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

class BaseISpec
    extends BaseSpec
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with HeaderNames
    with Status {
  import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, stubFor}
  import com.github.tomakehurst.wiremock.stubbing.StubMapping
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort
    ).build()

  protected lazy val contextRoot = "/individuals/tax-free-childcare/payments"
  protected lazy val baseUrl     = s"http://localhost:$port$contextRoot"

  protected def withAuthNinoRetrieval(check: => Assertion): Assertion = {
    expectAuthNinoRetrieval
    check
  }

  protected def expectAuthNinoRetrieval: StubMapping = stubFor(
    post("/auth/authorise") willReturn okJson(Json.obj("nino" -> "QW123456A").toString)
  )

  protected lazy val EXPECTED_JSON_ERROR_RESPONSE: ErrorResponse = ErrorResponse(
    statusCode = BAD_REQUEST,
    message = "Provided parameters do not match expected format."
  )

  protected lazy val CORRELATION_ID = "Correlation-ID"
}
