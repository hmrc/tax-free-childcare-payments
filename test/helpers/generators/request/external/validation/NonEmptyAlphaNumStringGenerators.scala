/*
 * Copyright 2026 HM Revenue & Customs
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

package helpers.generators.request.external.validation

import models.request.external.validation.NonEmptyAlphaNumString
import org.scalacheck.Gen

trait NonEmptyAlphaNumStringGenerators {

  private val MAX_PARAM_LEN = 16

  protected val genNonEmptyAlphaNumStrings: Gen[String] = for {
    len   <- Gen.chooseNum(1, MAX_PARAM_LEN)
    chars <- Gen.containerOfN[Array, Char](len, Gen.alphaNumChar)
  } yield chars.mkString

  protected val genNonEmptyAlphaNumStringValues: Gen[NonEmptyAlphaNumString] = for {
    string <- genNonEmptyAlphaNumStrings
  } yield NonEmptyAlphaNumString(string)

  protected val genInvalidNonEmptyAlphaNumStrings: Gen[String] =
    Gen.asciiPrintableStr.filterNot(NonEmptyAlphaNumString.regex.matches)

}
