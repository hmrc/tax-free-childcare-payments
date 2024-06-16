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

import models.requests.IdentifierRequest
import models.response.{BalanceResponse, PaymentResponse}
import org.scalacheck.{Arbitrary, Gen}
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.time.LocalDate
import java.util.UUID

trait Generators {
  import Arbitrary.arbitrary

  protected lazy val balanceResponses: Gen[BalanceResponse] = Gen const BalanceResponse("", 0, 0, 0, 0, 0)

  protected lazy val paymentResponses: Gen[PaymentResponse] = Gen const PaymentResponse("", LocalDate.now())

  protected lazy val nonEmptyAlphaNumStrings: Gen[String] = for {
    char0 <- Gen.alphaNumChar
    chars <- Gen.alphaNumStr
  } yield char0 +: chars

  implicit protected def arbIdentifierRequest[A: Arbitrary]: Arbitrary[IdentifierRequest[A]] =
    Arbitrary(
      for {
        nino <- ninos
        body <- arbitrary[A]
      } yield IdentifierRequest(nino, UUID.randomUUID(), FakeRequest("", "", Headers(), body))
    )

  protected lazy val ninos: Gen[String] = for {
    char0 <- Gen.alphaUpperChar
    char1 <- Gen.alphaUpperChar
    digits <- Gen.listOfN(6, Gen.numChar)
    char8 <- Gen.alphaUpperChar
  } yield char0 +: char1 +: digits.mkString :+ char8
}
