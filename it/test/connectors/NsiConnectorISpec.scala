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

import base.{BaseISpec, NsiStubs}
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.scenarios._
import models.requests.{IdentifierRequest, LinkRequest, PaymentRequest, SharedRequestData}
import models.response.NsiErrorResponse.{ETFC3, ETFC4}
import org.scalacheck.{Gen, Shrink}
import org.scalatest.EitherValues
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest

class NsiConnectorISpec extends BaseISpec with NsiStubs with EitherValues with models.request.Generators {
  private val connector = app.injector.instanceOf[NsiConnector]

  "method linkAccounts" should {
    s"respond $OK with a defined LinkResponse" when {
      s"NSI responds $CREATED with expected JSON format" in
        forAll { scenario: NsiLinkAccounts201Scenario =>
          stubNsiLinkAccounts201(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[LinkRequest] = scenario.identifierRequest

          val actualResponse = connector.linkAccounts.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(getRequestedFor(nsiLinkAccountsUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll { implicit req: IdentifierRequest[LinkRequest] =>
          stubNsiLinkAccountsError(BAD_REQUEST, "UNKNOWN", "An error occurred")

          val actualNsiErrorResponse = connector.linkAccounts.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }

  "method checkBalance" should {
    "return Right(BalanceResponse)" when {
      s"NSI responds $OK with expected JSON format" in
        forAll { scenario: NsiCheckBalance200Scenario =>
          stubNsiCheckBalance200(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[SharedRequestData] = scenario.identifierRequest

          val actualResponse = connector.checkBalance.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(getRequestedFor(nsiBalanceUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left ETFC3" when {
      implicit val shr: Shrink[String] = Shrink.shrinkAny

      "NSI responds with an invalid account status" in
        forAll(randomNinos, Gen.uuid, validSharedDataModels) { (nino, correlationId, sharedRequestData) =>
          implicit val req: IdentifierRequest[SharedRequestData] =
            IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), sharedRequestData))

          val invalidBalanceResponse = Json.obj(
            "accountStatus"  -> "unknown",
            "topUpAvailable" -> 1234,
            "topUpRemaining" -> 1234,
            "paidIn"         -> 1234,
            "totalBalance"   -> 1234,
            "clearedFunds"   -> 1234
          )

          stubFor {
            nsiCheckBalanceEndpoint
              .withQueryParams(nsiBalanceUrlQueryParams)
              .willReturn(created() withBody invalidBalanceResponse.toString)
          }

          val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC3
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll { implicit req: IdentifierRequest[SharedRequestData] =>
          stubNsiCheckBalanceError(BAD_REQUEST, "UNKNOWN", "An error occurred")

          val actualNsiErrorResponse = connector.checkBalance.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }

  "method makePayment" should {
    s"respond $OK with a defined PaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in
        forAll { scenario: NsiMakePayment201Scenario =>
          stubNsiMakePayment201(scenario.expectedRequestJson)

          implicit val req: IdentifierRequest[PaymentRequest] = scenario.identifierRequest

          val actualResponse = connector.makePayment.futureValue.value

          actualResponse shouldBe scenario.expectedResponse
          WireMock.verify(postRequestedFor(nsiPaymentUrlPattern).withHeader("Authorization", equalTo("Basic nsi-basic-token")))
        }
    }

    "return Left ETFC4" when {
      "NSI responds with unknown errorCode" in
        forAll { implicit req: IdentifierRequest[PaymentRequest] =>
          stubNsiMakePaymentError(BAD_REQUEST, "UNKNOWN", "An error occurred")

          val actualNsiErrorResponse = connector.makePayment.futureValue.left.value

          actualNsiErrorResponse shouldBe ETFC4
        }
    }
  }
}
