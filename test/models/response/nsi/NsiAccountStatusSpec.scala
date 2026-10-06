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

package models.response.nsi

import helpers.BaseSpec
import models.response.external.ExternalAccountStatus
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.prop.TableFor3
import play.api.libs.json.*

class NsiAccountStatusSpec extends BaseSpec {

  private val accountStatusScenarios: TableFor3[JsValue, NsiAccountStatus, ExternalAccountStatus] =
    Table[JsValue, NsiAccountStatus, ExternalAccountStatus](
      ("Expected NSI JSON", "NSI Account Status", "Expected External Account Status"),
      (JsString("ACTIVE"), NsiAccountStatus.ACTIVE, ExternalAccountStatus.ACTIVE),
      (JsString("BLOCKED"), NsiAccountStatus.BLOCKED, ExternalAccountStatus.INACTIVE)
    )

  private val randomInvalidNsiAccountStatusJson: Gen[JsValue] = Gen.oneOf(
    arbitrary[BigDecimal].map(JsNumber.apply),
    arbitrary[Boolean].map(JsBoolean.apply),
    Gen.const(Json.obj()),
    Gen.const(Json.arr()),
    Gen.const(JsNull)
  )

  private val randomInvalidNsiAccountStatusJsonString: Gen[JsString] =
    Gen.oneOf("active", "blocked", "unknown").map(JsString(_))

  "reads" should {

    "return JsSuccess" when {
      "json is a valid string" in
        forAll(accountStatusScenarios) { (expectedNsiJson, expectedNsiAccountStatus, _) =>
          val actualNsiAccountStatus = expectedNsiJson.validate[NsiAccountStatus].asEither.value

          actualNsiAccountStatus shouldBe expectedNsiAccountStatus
        }
    }

    "return JsError" when {
      "json is not a string" in
        forAll(randomInvalidNsiAccountStatusJson) { invalidJson =>
          val (_, jsonValidationErrors) = invalidJson.validate[NsiAccountStatus].asEither.left.value.loneElement

          val jsonValidationError = jsonValidationErrors.loneElement.message

          jsonValidationError shouldBe "error.expected.account_status.string"
        }

      "json is an invalid string" in
        forAll(randomInvalidNsiAccountStatusJsonString) { invalidJson =>
          val (_, jsonValidationErrors) = invalidJson.validate[NsiAccountStatus].asEither.left.value.loneElement

          val jsonValidationError = jsonValidationErrors.loneElement.message

          jsonValidationError shouldBe "error.invalid.account_status"
        }
    }
  }

  "toExternalAccountStatus" should {
    "return correct values" in
      forAll(accountStatusScenarios) { (_, nsiAccountStatus, externalAccountStatus) =>
        nsiAccountStatus.toExternalAccountStatus shouldBe externalAccountStatus
      }
  }

}
