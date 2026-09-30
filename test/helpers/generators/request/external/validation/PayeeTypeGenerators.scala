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

import models.request.external.validation.PayeeType
import org.scalacheck.Gen

trait PayeeTypeGenerators {

  protected val genPayeeTypes: Gen[PayeeType] = Gen.oneOf(PayeeType.values.toSeq)

  protected val genPayeeTypeStrings: Gen[String] = for {
    payeeType <- genPayeeTypes
  } yield payeeType.toString

  protected val genInvalidPayeeTypeStrings: Gen[String] = Gen.asciiPrintableStr.filterNot(_ == "CCP")

}
