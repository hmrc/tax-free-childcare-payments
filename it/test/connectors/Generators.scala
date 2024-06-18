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

import org.scalacheck.Gen

trait Generators {

  protected lazy val nonEmptyAlphaNumStrings: Gen[String] = for {
    char0 <- Gen.alphaNumChar
    chars <- Gen.alphaNumStr
  } yield char0 +: chars

  protected lazy val ninos: Gen[String] = for {
    char0 <- Gen.alphaUpperChar
    char1 <- Gen.alphaUpperChar
    digits <- Gen.listOfN(6, Gen.numChar)
    char8 <- Gen.alphaUpperChar
  } yield char0 +: char1 +: digits.mkString :+ char8
}
