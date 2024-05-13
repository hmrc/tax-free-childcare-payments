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
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.Json

class AuthActionISpec extends BaseISpec with TableDrivenPropertyChecks {

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
        s"respond $BAD_REQUEST" when {
          s"request header $CORRELATION_ID is missing" in withAuthNinoRetrieval {
            val res = wsClient
              .url(url)
              .withHttpHeaders(
                AUTHORIZATION -> "Bearer qwertyuiop"
              )
              .post(payload)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe Json.obj()
          }

          s"request header $CORRELATION_ID is not a valid UUID" in withAuthNinoRetrieval {
            val res = wsClient
              .url(url)
              .withHttpHeaders(
                AUTHORIZATION  -> "Bearer qwertyuiop",
                CORRELATION_ID -> "I am an invalid UUID."
              )
              .post(payload)
              .futureValue

            res.status shouldBe BAD_REQUEST
            res.json shouldBe Json.obj()
          }
        }
      }
    }
  }
}
