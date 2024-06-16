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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{created, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.requests.{IdentifierRequest, LinkRequest, SharedRequestData}
import models.response.LinkResponse
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.time.LocalDate
import java.util.UUID

final case class NsiLinkAccounts201Scenario(
    correlationId: UUID,
    childAccountPaymentRef: String,
    eppURN: String,
    eppAccount: String,
    parentNino: String,
    childDoB: LocalDate,
    expectedLinkResponse: LinkResponse
  ) {

  def stubNsiResponse(): StubMapping = stubFor {
    val body = Json.obj("childFullName" -> expectedLinkResponse.child_full_name)

    WireMock.post(s"/account/v1/accounts/link-to-epp/$childAccountPaymentRef") willReturn
      created().withBody(body.toString)
  }

  val identifierRequest: IdentifierRequest[LinkRequest] = {
    val sharedRequestData = SharedRequestData(eppAccount, eppURN, childAccountPaymentRef)

    IdentifierRequest(
      parentNino,
      correlationId,
      FakeRequest("", "", Headers(), LinkRequest(sharedRequestData, childDoB))
    )
  }
}

object NsiLinkAccounts201Scenario extends Generators {

  implicit val arb: Arbitrary[NsiLinkAccounts201Scenario] = Arbitrary(
    for {
      correlationId          <- Gen.uuid
      childAccountPaymentRef <- nonEmptyAlphaNumStrings
      eppURN                 <- nonEmptyAlphaNumStrings
      eppAccount             <- nonEmptyAlphaNumStrings
      parentNino             <- ninos
      childAgeDays           <- Gen.chooseNum(1, 18 * 365)
      expectedLinkResponse   <- linkResponses
    } yield apply(
      correlationId,
      childAccountPaymentRef,
      eppURN,
      eppAccount,
      parentNino,
      LocalDate.now() minusDays childAgeDays,
      expectedLinkResponse
    )
  )

  private lazy val linkResponses = fullNames map LinkResponse.apply

  private lazy val fullNames = for {
    firstName <- names
    lastName  <- names
  } yield s"$firstName $lastName"

  private lazy val names = for {
    char0 <- Gen.alphaUpperChar
    char1 <- Gen.alphaLowerChar
    chars <- Gen.alphaLowerStr
  } yield char0 +: char1 +: chars
}
