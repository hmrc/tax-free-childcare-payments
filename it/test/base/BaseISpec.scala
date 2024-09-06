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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Assertion, LoneElement}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status
import play.api.libs.ws.WSResponse
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

abstract class BaseISpec(enablePayeeTypeEPP: Boolean = false)
    extends BaseSpec
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WsTestClient
    with utils.HeaderNames
    with Status
    with TableDrivenPropertyChecks
    with ScalaCheckPropertyChecks
    with LogCapturing
    with LoneElement {
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort,
      "features.enablePayeeTypeEPP"     -> enablePayeeTypeEPP
    ).build()

  protected def checkSecurityPolicy(response: WSResponse): Assertion = {
    val actualReferrerPolicy    = response.header(REFERRER_POLICY).value
    val actualPermissionsPolicy = response
      .header(PERMISSIONS_POLICY)
      .value
      .split(", ")
      .collect { case directive(feature, allowlist) => feature -> allowlist }
      .toMap

    actualReferrerPolicy    shouldBe expectedReferrerPolicy
    actualPermissionsPolicy shouldBe expectedPermissionsPolicy
  }

  private val directive = "^([a-z-]+)=([^=]+)$".r

  private val expectedReferrerPolicy = "no-referrer"
  private val expectedPermissionsPolicy = Map(
    "camera"                    -> "()",
    "fullscreen"                -> "()",
    "geolocation"               -> "()",
    "hid"                       -> "()",
    "microphone"                -> "()",
    "publickey-credentials-get" -> "()",
    "screen-wake-lock"          -> "()",
    "web-share"                 -> "()"
  )

  protected lazy val baseUrl = s"http://localhost:$port"

  protected lazy val EXPECTED_CORRELATION_ID_ERROR_DESC      = "Correlation ID is in an invalid format or is missing"
  protected lazy val EXPECTED_AUTH_NINO_RETRIEVAL_ERROR_DESC = "Bearer Token did not return a valid record"
}
