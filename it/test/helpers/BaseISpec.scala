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

package helpers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import helpers.json.TestFormats
import org.scalatest.LoneElement
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsObject
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
    with HeaderNames
    with Status
    with TableDrivenPropertyChecks
    with ScalaCheckPropertyChecks
    with LogCapturing
    with TestFormats
    with LoneElement {

  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.json.Json

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.nsi.port"  -> wireMockPort,
        "features.enablePayeeTypeEPP"     -> enablePayeeTypeEPP
      )
      .build()

  protected val baseUrl = s"http://localhost:$port"

  protected val CORRELATION_ID = "Correlation-ID"

  protected def errorAsJson(
      errorCode: String,
      errorDescription: String
  ): JsObject =
    Json.obj(
      "errorCode"        -> errorCode,
      "errorDescription" -> errorDescription
    )

  protected def checkLoneLog(expectedLevel: Level, expectedMessage: String)(logs: List[ILoggingEvent]): Unit = {
    val log = logs.loneElement

    (log.getLevel, log.getMessage) shouldBe (expectedLevel, expectedMessage)
  }

}
