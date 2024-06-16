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

import models.response.{BalanceResponse, LinkResponse, PaymentResponse}
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.LocalDate

trait NsiStubs extends ScalaCheckPropertyChecks {
  import com.github.tomakehurst.wiremock.client.WireMock._
  import connectors.NsiStubs._

  protected def forAllLinkAccounts201Scenarios(doCheck: (String, LinkResponse) => Assertion): Unit =
    forAll(nonEmptyAlphaNumStrings, linkResponses) { (accountRef, expectedResponse) =>
      stubFor {
        linkAccountsEndpoint(accountRef) willReturn created().withBody(expectedResponse.toString)
      }
      doCheck(accountRef, expectedResponse)
    }

  protected def forAllCheckBalance200Scenarios(doCheck: (String, BalanceResponse) => Assertion): Unit =
    forAll(nonEmptyAlphaNumStrings, balanceResponses) { (accountRef, expectedResponse) =>
      stubFor {
        checkBalanceEndpoint(accountRef) willReturn okJson(expectedResponse.toString)
      }
      doCheck(accountRef, expectedResponse)
    }

  protected def forAllNsiLinkAccounts201Scenarios(doCheck: (String, PaymentResponse) => Assertion): Unit =
    forAll(nonEmptyAlphaNumStrings, paymentResponses) { (accountRef, expectedResponse) =>
      stubFor {
        makePaymentEndpoint(accountRef) willReturn created().withBody(expectedResponse.toString)
      }
      doCheck(accountRef, expectedResponse)
    }

  private lazy val linkResponses = fullNames map LinkResponse.apply

  private lazy val balanceResponses = Gen const BalanceResponse("", 0, 0, 0, 0, 0)

  private lazy val paymentResponses = Gen const PaymentResponse("", LocalDate.now())

  private lazy val names = for {
    char0 <- Gen.alphaUpperChar
    char1 <- Gen.alphaLowerChar
    chars <- Gen.alphaLowerStr
  } yield char0 +: char1 +: chars

  private lazy val fullNames = for {
    firstName <- names
    lastName  <- names
  } yield s"$firstName $lastName"

  private lazy val nonEmptyAlphaNumStrings = for {
    char0 <- Gen.alphaNumChar
    chars <- Gen.alphaNumStr
  } yield char0 +: chars
}

object NsiStubs {
  import com.github.tomakehurst.wiremock.client.WireMock

  private def linkAccountsEndpoint(accountRef: String) =
    WireMock.post(s"/account/v1/accounts/link-to-epp/$accountRef")

  private def checkBalanceEndpoint(accountRef: String) =
    WireMock.get(s"/account/v1/accounts/link-to-epp/$accountRef")

  private def makePaymentEndpoint(accountRef: String) =
    WireMock.post(s"/account/v1/accounts/link-to-epp/$accountRef")
}
