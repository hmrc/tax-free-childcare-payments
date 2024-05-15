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

package controllers

import base.BaseISpec
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, stubFor}
import controllers.actions.AuthAction
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.Future

class AuthActionISpec extends BaseISpec with TableDrivenPropertyChecks with LogCapturing {

  withClient { wsClient =>
    val resources = Table(
      "URL"               -> "Valid Payload",
      s"$baseUrl/link"    -> randomLinkRequestJson,
      s"$baseUrl/balance" -> randomMetadataJson,
      s"$baseUrl/"        -> randomPaymentRequestJson
    )

    /** Covers `case None` of [[controllers.actions.AuthAction.invokeBlock().]] */
    forAll(resources) { (url, payload) =>
      s"POST $url" should {
        s"respond $BAD_REQUEST and give expected error message" when {
          s"Auth service does not return a nino" in
            expect400With("Unable to retrieve NI number.") {
              stubFor(
                post("/auth/authorise") willReturn okJson("{}")
              )

              wsClient
                .url(url)
                .withHttpHeaders(
                  AUTHORIZATION -> "Bearer qwertyuiop"
                )
                .post(payload)
            }

          s"request header $CORRELATION_ID is missing" in
            expect400With("Correlation ID is missing.") {
              expectAuthNinoRetrieval

              wsClient
                .url(url)
                .withHttpHeaders(
                  AUTHORIZATION -> "Bearer qwertyuiop"
                )
                .post(payload)
            }

          val invalidUuid = "asdfghkj"

          s"request header $CORRELATION_ID is not a valid UUID" in
            expect400With("Invalid UUID string: " + invalidUuid) {
              expectAuthNinoRetrieval

              wsClient
                .url(url)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalidUuid
                )
                .post(payload)
            }
        }
      }
    }

    def expect400With(message: String)(block: Future[WSRequest#Self#Response]): Unit =
      withCaptureOfLoggingFrom(Logger(classOf[AuthAction])) { logs =>
        val response             = block.futureValue
        val expectedResponseBody = Json.obj(
          "statusCode" -> BAD_REQUEST,
          "message"    -> message
        )

        response.status shouldBe BAD_REQUEST
        response.json shouldBe expectedResponseBody

        val log = logs.last

        log.getLevel shouldBe Level.INFO
        log.getMessage shouldBe message
      }
  }
}
