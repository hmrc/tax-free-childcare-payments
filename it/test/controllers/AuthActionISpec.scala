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
import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Logger
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.util.UUID

class AuthActionISpec extends BaseISpec with TableDrivenPropertyChecks with LogCapturing {

  withClient { wsClient =>
    val resources = Table(
      ("Endpoint Name", "URL", "Valid Payload"),
      ("link", s"$resourcePath/link", randomLinkRequestJson),
      ("balance", s"$resourcePath/balance", randomSharedJson),
      ("payment", s"$resourcePath/", randomPaymentRequestJson)
    )

    /** Covers `case None` of [[controllers.actions.AuthAction.invokeBlock().]] */
    forAll(resources) { (endpointName, resource, payload) =>
      val endpoint = s"POST $resource"

      endpoint should {
        s"respond $BAD_REQUEST and give expected error message" when {
          s"request header $CORRELATION_ID is missing" in
            withEmptyNinoRetrievalAndLogCheck(endpointName, "null", "Missing Correlation-ID header") {
              val response = wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION -> "Bearer qwertyuiop"
                )
                .post(payload)
                .futureValue

              check(
                BAD_REQUEST,
                "BAD_REQUEST",
                "Correlation-ID header is invalid or missing" ,
                response
              )
            }

          val invalidUuid = "asdfghkj"

          s"request header $CORRELATION_ID is not a valid UUID" in
            withEmptyNinoRetrievalAndLogCheck(endpointName, invalidUuid, s"Invalid UUID string: $invalidUuid") {
              val response  = wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> invalidUuid
                )
                .post(payload)
                .futureValue

              check(
                BAD_REQUEST,
                "BAD_REQUEST",
                "Correlation-ID header is invalid or missing" ,
                response
              )
            }

          val validCorrelationId = UUID.randomUUID().toString

          s"Auth service does not return a nino" in
            withEmptyNinoRetrievalAndLogCheck(endpointName, validCorrelationId, "Unable to retrieve NI number") {
              val response = wsClient
                .url(domain + resource)
                .withHttpHeaders(
                  AUTHORIZATION  -> "Bearer qwertyuiop",
                  CORRELATION_ID -> validCorrelationId
                )
                .post(payload)
                .futureValue

              check(
                INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "The server encountered an error and couldn't process the request",
                response
              )
            }
        }
      }
    }

    def withEmptyNinoRetrievalAndLogCheck(
        expectedEndpoint: String,
        expectedCorrelationId: String,
        expectedErrorMessage: String
      )(
        doCheck: => Assertion
      ): Unit =
      withCaptureOfLoggingFrom(Logger(classOf[AuthAction])) { logs =>
        stubFor(
          post("/auth/authorise") willReturn okJson("{}")
        )

        doCheck

        val log = logs.last

        log.getLevel shouldBe Level.INFO
        log.getMessage match {
          case EXPECTED_LOG_MESSAGE_PATTERN(loggedEndpoint, loggedCorrelationId, loggedMessage) =>
            loggedEndpoint shouldBe expectedEndpoint
            loggedCorrelationId shouldBe expectedCorrelationId
            loggedMessage shouldBe expectedErrorMessage

          case other => fail(s"$other did not match $EXPECTED_LOG_MESSAGE_PATTERN")
        }
      }

    def check(
               expectedStatus: Int,
               expectedCode: String,
               expectedDescription: String,
               response: WSRequest#Self#Response
             ): Assertion = {
      response.status shouldBe expectedStatus

      val actualCode = (response.json \ "errorCode").as[String]
      val actualDescription = (response.json \ "errorDescription").as[String]

      actualCode shouldBe expectedCode
      actualDescription shouldBe expectedDescription
    }
  }
}
