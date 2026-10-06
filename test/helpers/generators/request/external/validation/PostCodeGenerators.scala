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

import models.request.external.validation.PostCode
import org.scalacheck.Gen

trait PostCodeGenerators {

  private val randomSpaces: Gen[String] = Gen.stringOf(Gen.const(' '))

  protected val genPostCodeStrings: Gen[String] = for {
    leadingSpaces  <- randomSpaces
    n              <- Gen.chooseNum(1, 2)
    letters1       <- Gen.stringOfN(n, Gen.alphaUpperChar)
    num1           <- Gen.chooseNum(1, 99)
    midSpaces      <- randomSpaces
    num2           <- Gen.chooseNum(1, 9)
    letters2       <- Gen.stringOfN(2, Gen.alphaUpperChar)
    trailingSpaces <- randomSpaces
  } yield s"$leadingSpaces$letters1$num1$midSpaces$num2$letters2$trailingSpaces"

  protected val genPostCodes: Gen[PostCode] = for {
    postCodeString <- genPostCodeStrings
  } yield PostCode(postCodeString)

  protected val genInvalidPostCodeStrings: Gen[String] = Gen.asciiPrintableStr.filterNot(PostCode.regex.matches)

}
