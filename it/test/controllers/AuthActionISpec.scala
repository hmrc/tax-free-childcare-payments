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

import java.util.UUID
import scala.concurrent.Future

class AuthActionISpec extends BaseISpec with TableDrivenPropertyChecks with LogCapturing {

  withClient { wsClient =>
    val resources = Table(
      "URL"                    -> "Valid Payload",
      s"$resourcePath/link"    -> randomLinkRequestJson,
      s"$resourcePath/balance" -> randomSharedJson,
      s"$resourcePath/"        -> randomPaymentRequestJson
    )

    /** Covers `case None` of [[controllers.actions.AuthAction.invokeBlock().]] */
    forAll(resources) { (resource, payload) =>
      s"POST $resource" should {
        s"respond $BAD_REQUEST and give expected error message" when {
          s"request header $CORRELATION_ID is missing" in
            expect400With("<null> Missing Correlation-ID header.") {
              expectAuthNinoRetrieval

              wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION -> "Bearer qwertyuiop"
                )
                .post(payload)
            }

          val invalidUuid = "asdfghkj"

          s"request header $CORRELATION_ID is not a valid UUID" in
            expect400With(s"<$invalidUuid> Invalid UUID string: $invalidUuid") {
              expectAuthNinoRetrieval

              wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalidUuid
                )
                .post(payload)
            }

          val validCorrelationId = UUID.randomUUID().toString

          s"Auth service does not return a nino" in
            expect400With(s"<$validCorrelationId> Unable to retrieve NI number.") {
              stubFor(
                post("/auth/authorise") willReturn okJson("{}")
              )

              wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> validCorrelationId
                )
                .post(payload)
            }
        }
      }
    }

    def expect400With(expectedErrorMessage: String)(block: Future[WSRequest#Self#Response]): Unit =
      withCaptureOfLoggingFrom(Logger(classOf[AuthAction])) { logs =>
        val response             = block.futureValue
        val expectedResponseBody = Json.obj(
          "statusCode" -> BAD_REQUEST,
          "message"    -> expectedErrorMessage
        )

        response.status shouldBe BAD_REQUEST
        response.json shouldBe expectedResponseBody

        val log = logs.last

        log.getLevel shouldBe Level.INFO
        log.getMessage shouldBe expectedErrorMessage
      }
  }
}
