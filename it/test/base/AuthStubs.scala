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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, okJson, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalacheck.Gen
import play.api.libs.json.Json
import play.api.test.Helpers.UNAUTHORIZED
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser

trait AuthStubs {

  protected def stubAuthRetrievalOf(nino: String): StubMapping = stubFor {
    WireMock
      .post("/auth/authorise")
      .willReturn(
        okJson(
          Json
            .obj(
              "nino"            -> nino,
              "confidenceLevel" -> randomCL
            )
            .toString
        )
      )
  }

  protected def stubAuthEmptyRetrieval: StubMapping = stubFor {
    WireMock
      .post("/auth/authorise")
      .willReturn(
        okJson(
          Json
            .obj(
              "confidenceLevel" -> randomCL
            )
            .toString
        )
      )
  }

  protected def stubAuthWithLowCL: StubMapping = stubFor {
    WireMock
      .post("/auth/authorise")
      .willReturn(
        aResponse()
          .withStatus(UNAUTHORIZED)
          .withHeader(AuthenticateHeaderParser.WWW_AUTHENTICATE, """MDTP detail="InsufficientConfidenceLevel""""
          )
      )
  }

  private def randomCL = Gen.oneOf(200, 250).sample.get
}
