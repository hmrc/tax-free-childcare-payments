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

import org.scalatest.OptionValues
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

import play.api.libs.json.{JsObject, Json}

class BaseSpec
    extends AnyWordSpec
    with should.Matchers
    with OptionValues {

  protected def randomLinkRequestJson: JsObject =
    randomSharedJson ++ Json.obj(
      "child_date_of_birth" -> randomDateOfBirth
    )

  protected def randomPaymentRequestJson: JsObject =
    randomSharedJson ++ Json.obj(
      "payment_amount"    -> randomSumOfMoney,
      "ccp_reg_reference" -> randomRegistrationRef,
      "ccp_postcode"      -> "AB12 3CD",
      "payee_type"        -> randomPayeeType
    )

  protected def randomSharedJson: JsObject = Json.obj(
    "epp_unique_customer_id"     -> randomCustomerId,
    "epp_reg_reference"          -> randomRegistrationRef,
    "outbound_child_payment_ref" -> randomPaymentRef
  )

  protected def randomCustomerId: String      = randomStringOf(EXPECTED_CUSTOMER_ID_LENGTH, '0' to '9')
  protected def randomRegistrationRef: String = randomStringOf(EXPECTED_REGISTRATION_REF_LENGTH, ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))

  protected def randomPaymentRef: String = {
    val letters = randomStringOf(EXPECTED_PAYMENT_REF_LETTERS, 'A' to 'Z')
    val digits  = randomStringOf(EXPECTED_PAYMENT_REF_DIGITS, '0' to '9')

    letters + digits + "TFC"
  }

  protected def randomPayeeType: String = Seq("CCP", "EPP")(Random.nextInt(2))

  private def randomStringOf(n: Int, chars: Seq[Char]) = {
    def randomChar = chars(Random.nextInt(chars.length))
    Array.fill(n)(randomChar).mkString
  }

  private def randomSumOfMoney = BigDecimal(Random.nextInt(MAX_AMOUNT_OF_PENCE) / 100).setScale(2)

  protected def randomDateOfBirth: LocalDate = LocalDate.now() minusDays Random.nextInt(MAX_CHILD_AGE_DAYS)
  protected def randomPaymentDate: LocalDate = LocalDate.now() plusDays Random.nextInt(MAX_PAYMENT_DELAY_DAYS)

  private val EXPECTED_CUSTOMER_ID_LENGTH      = 11
  private val EXPECTED_REGISTRATION_REF_LENGTH = 16
  private val EXPECTED_PAYMENT_REF_LETTERS     = 4
  private val EXPECTED_PAYMENT_REF_DIGITS      = 5

  private val MAX_AMOUNT_OF_PENCE    = 1000000
  private val MAX_CHILD_AGE_DAYS     = 18 * 365
  private val MAX_PAYMENT_DELAY_DAYS = 30
}
