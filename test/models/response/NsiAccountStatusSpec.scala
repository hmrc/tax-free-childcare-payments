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

package models.response

import base.BaseSpec
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.libs.json._

class NsiAccountStatusSpec extends BaseSpec {
  "writesToApi" should {
    "return expected API JSON" in
      forAll(accountStatusScenarios) {
        (_, nsiAccountStatus, expectedApiJson) =>
          val actualApiJson = Json.toJson(nsiAccountStatus)

          actualApiJson shouldBe expectedApiJson
      }
  }

  "readsFromNsi" should {
    "return JsSuccess" in
      forAll(accountStatusScenarios) {
        (expectedNsiJson, expectedNsiAccountStatus, _) =>
          val actualNsiAccountStatus = expectedNsiJson.validate[NsiAccountStatus].asEither.value

          actualNsiAccountStatus shouldBe expectedNsiAccountStatus
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

  private lazy val accountStatusScenarios = Table[JsValue, NsiAccountStatus, JsValue](
    ("Expected NSI JSON", "NSI Account Status", "Expected API JSON"),
    (JsString("ACTIVE"), NsiAccountStatus.ACTIVE, JsString("ACTIVE")),
    (JsString("BLOCKED"), NsiAccountStatus.BLOCKED, JsString("INACTIVE"))
  )

  private lazy val randomInvalidNsiAccountStatusJson = Gen.oneOf(
    arbitrary[BigDecimal] map JsNumber.apply,
    arbitrary[Boolean] map JsBoolean.apply,
    Gen const Json.obj(),
    Gen const Json.arr(),
    Gen const JsNull
  )

  private lazy val randomInvalidNsiAccountStatusJsonString =
    Gen.oneOf("active", "blocked", "unknown") map JsString.apply
}
