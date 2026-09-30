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

import models.request.external.validation.ChildAccountRef
import org.scalacheck.Gen

trait ChildAccountRefGenerators {

  protected val genChildAccountRefStrings: Gen[String] = for {
    letters <- Gen.stringOfN(ChildAccountRef.Letters, Gen.alphaChar)
    specialCharacter <- Gen.stringOfN(
      ChildAccountRef.LettersOrSpecialChars,
      Gen.oneOf(Gen.alphaChar, Gen.oneOf("0.'- "))
    )
    digits <- Gen.stringOfN(ChildAccountRef.Digits, Gen.numChar)
  } yield s"$letters$specialCharacter${digits}TFC"

  protected val genChildAccountRefs: Gen[ChildAccountRef] = for {
    refString <- genChildAccountRefStrings
  } yield ChildAccountRef(refString)

  protected val genInvalidChildAccountRefStrings: Gen[String] =
    Gen.asciiPrintableStr.filterNot(ChildAccountRef.regex.matches)

}
