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

import models.request.Payee
import models.request.Payee.ChildCareProvider
import org.scalacheck.Gen

trait Generators {

  protected lazy val fullNames: Gen[String] = for {
    firstName <- names
    lastName  <- names
  } yield s"$firstName $lastName"

  private lazy val names = for {
    char0 <- Gen.alphaUpperChar
    char1 <- Gen.alphaLowerChar
    chars <- Gen.alphaLowerStr
  } yield char0 +: char1 +: chars

  protected lazy val nonEmptyAlphaNumStrings: Gen[String] = for {
    len   <- Gen.chooseNum(1, MAX_PARAM_LEN)
    chars <- Gen.containerOfN[Array, Char](len, Gen.alphaNumChar)
  } yield chars.mkString

  private lazy val MAX_PARAM_LEN = 16

  protected lazy val randomNinos: Gen[String] = for {
    char0  <- Gen.alphaUpperChar
    char1  <- Gen.alphaUpperChar
    digits <- Gen.listOfN(6, Gen.numChar)
    char8  <- Gen oneOf "ABCD"
  } yield char0 +: char1 +: digits.mkString :+ char8

  protected lazy val randomPayees: Gen[Payee] = Gen.oneOf(
    Gen const Payee.ExternalPaymentProvider,
    childCareProviders
  )

  protected lazy val childCareProviders: Gen[ChildCareProvider] = for {
    urn      <- nonEmptyAlphaNumStrings
    postcode <- postcodes
  } yield ChildCareProvider(urn, postcode)

  lazy private val postcodes = for {
    leadingSpaces <- randomSpaces
    n        <- Gen.chooseNum(1, 2)
    letters1 <- Gen.stringOfN(n, Gen.alphaUpperChar)
    num1     <- Gen.chooseNum(1, 99)
    midSpaces <- randomSpaces
    num2     <- Gen.chooseNum(1, 9)
    letters2 <- Gen.stringOfN(2, Gen.alphaUpperChar)
    trailingSpaces <- randomSpaces
  } yield s"$leadingSpaces$letters1$num1$midSpaces$num2$letters2$trailingSpaces"

  lazy private val randomSpaces = Gen.stringOf(Gen const ' ')
}
