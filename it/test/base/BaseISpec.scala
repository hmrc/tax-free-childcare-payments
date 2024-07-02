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

import ch.qos.logback.classic.Level
import config.CustomJsonErrorHandler
import org.scalatest.LoneElement
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Logger
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsValue
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

abstract class BaseISpec
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
    with LoneElement {
  import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, stubFor}
  import org.scalatest.Assertion
  import play.api.Application
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.json.Json

  import scala.util.matching.Regex

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.nsi.port"  -> wireMockPort
    ).build()

  protected lazy val baseUrl = s"http://localhost:$port"

  protected def withAuthNinoRetrieval(check: => Assertion): Assertion = {
    stubFor(
      post("/auth/authorise") willReturn okJson(Json.obj("nino" -> "QW123456A").toString)
    )
    check
  }

  protected def withAuthNinoRetrievalExpectLog(
      expectedEndpoint: String,
      expectedCorrelationId: String
    )(
      doTest: => Assertion
    ): Unit = {
    withCaptureOfLoggingFrom(
      Logger(classOf[CustomJsonErrorHandler])
    ) { logs =>
      withAuthNinoRetrieval {
        doTest
      }

      val log = logs.loneElement
      log.getLevel shouldBe Level.INFO
      log.getMessage match {
        case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
          loggedEndpoint shouldBe expectedEndpoint
          loggedCorrelationId shouldBe expectedCorrelationId
          loggedMessage should startWith("Json validation error")

        case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
      }
    }
  }

  protected lazy val EXPECTED_LOG_MESSAGE_PATTERN: Regex =
    raw"^\[Error] - \[([^]]+)] - \[([^:]+): (.+)]$$".r

  protected lazy val EXPECTED_JSON_ERROR_RESPONSE: JsValue = Json.obj(
    "errorCode"        -> "BAD_REQUEST",
    "errorDescription" -> "Request data is invalid or missing"
  )

  protected lazy val CORRELATION_ID = "Correlation-ID"
}
