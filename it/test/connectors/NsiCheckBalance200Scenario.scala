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
import com.github.tomakehurst.wiremock.client.WireMock.{okJson, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.requests.{IdentifierRequest, SharedRequestData}
import models.response.{AccountStatus, BalanceResponse}
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.util.UUID

final case class NsiCheckBalance200Scenario(
    correlationId: UUID,
    childAccountPaymentRef: String,
    eppURN: String,
    eppAccount: String,
    parentNino: String,
    expectedResponse: BalanceResponse
  ) {

  def stubNsiResponse(): StubMapping = stubFor {
    val body = Json.obj(
      "accountStatus"  -> expectedResponse.accountStatus,
      "topUpAvailable" -> expectedResponse.topUpAvailable,
      "topUpRemaining" -> expectedResponse.topUpRemaining,
      "paidIn"         -> expectedResponse.paidIn,
      "totalBalance"   -> expectedResponse.totalBalance,
      "clearedFunds"   -> expectedResponse.clearedFunds
    )

    WireMock.get(s"/account/v1/accounts/balance/$childAccountPaymentRef") willReturn okJson(body.toString)
  }

  val identifierRequest: IdentifierRequest[SharedRequestData] = {
    val sharedRequestData = SharedRequestData(eppAccount, eppURN, childAccountPaymentRef)

    IdentifierRequest(
      parentNino,
      correlationId,
      FakeRequest("", "", Headers(), sharedRequestData)
    )
  }
}

object NsiCheckBalance200Scenario extends Generators {

  implicit val arb: Arbitrary[NsiCheckBalance200Scenario] = Arbitrary(
    for {
      correlationId          <- Gen.uuid
      childAccountPaymentRef <- nonEmptyAlphaNumStrings
      eppURN                 <- nonEmptyAlphaNumStrings
      eppAccount             <- nonEmptyAlphaNumStrings
      parentNino             <- ninos
      expectedResponse       <- balanceResponses
    } yield apply(
      correlationId,
      childAccountPaymentRef,
      eppURN,
      eppAccount,
      parentNino,
      expectedResponse
    )
  )

  private lazy val balanceResponses = for {
    accountStatus  <- Gen oneOf AccountStatus.values
    topUpAvailable <- Gen.chooseNum(0, Int.MaxValue)
    topUpRemaining <- Gen.chooseNum(0, Int.MaxValue)
    paidIn         <- Gen.chooseNum(0, Int.MaxValue)
    totalBalance   <- Gen.chooseNum(0, Int.MaxValue)
    clearedFunds   <- Gen.chooseNum(0, Int.MaxValue)
  } yield BalanceResponse(accountStatus, topUpAvailable, topUpRemaining, paidIn, totalBalance, clearedFunds)
}
