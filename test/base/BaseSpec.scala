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

import java.time.LocalDate
import scala.util.Random

import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import play.api.libs.json._

class BaseSpec
    extends AnyWordSpec
    with should.Matchers
    with OptionValues
    with ScalaCheckPropertyChecks {

  protected def randomCustomerId: String      = randomStringOf(EXPECTED_CUSTOMER_ID_LENGTH, '0' to '9')
  protected def randomRegistrationRef: String = randomStringOf(EXPECTED_REGISTRATION_REF_LENGTH, ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))

  protected def randomOutboundChildPaymentRef: String = {
    val letters = randomStringOf(EXPECTED_PAYMENT_REF_LETTERS, 'A' to 'Z')
    val digits  = randomStringOf(EXPECTED_PAYMENT_REF_DIGITS, '0' to '9')

    letters + digits + "TFC"
  }

  protected def randomPayeeType: String = Seq("CCP", "EPP")(Random.nextInt(2))

  private def randomStringOf(n: Int, chars: Seq[Char]) = {
    def randomChar = chars(Random.nextInt(chars.length))
    Array.fill(n)(randomChar).mkString
  }

  protected def randomPaymentDate: LocalDate = LocalDate.now() plusDays Random.nextInt(MAX_PAYMENT_DELAY_DAYS)

  private val EXPECTED_CUSTOMER_ID_LENGTH      = 11
  private val EXPECTED_REGISTRATION_REF_LENGTH = 16
  private val EXPECTED_PAYMENT_REF_LETTERS     = 4
  private val EXPECTED_PAYMENT_REF_DIGITS      = 5

  private val MAX_PAYMENT_DELAY_DAYS = 30

  protected def checkErrorJson(
      actualJson: => JsValue,
      expectedErrorCode: String,
      expectedErrorDescription: String
    ): Assertion = {
    actualJson \ "errorCode" shouldBe JsDefined(JsString(expectedErrorCode))
    actualJson \ "errorDescription" shouldBe JsDefined(JsString(expectedErrorDescription))
  }

  protected lazy val EXPECTED_400_ERROR_DESCRIPTION =
    "Request data is invalid or missing. Please refer to API Documentation for further information"

  protected lazy val EXPECTED_500_ERROR_DESCRIPTION =
    "The server encountered an error and couldn't process the request. Please refer to API Documentation for further information"
}
