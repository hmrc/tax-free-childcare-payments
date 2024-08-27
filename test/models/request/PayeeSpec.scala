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

package models.request

import base.BaseSpec
import models.request.data.PayeeGenerators
import play.api.libs.json.{KeyPathNode, Reads}

class PayeeSpec extends BaseSpec with PayeeGenerators {
  "val readsPayeeFromApi" should {
    implicit val reads: Reads[Payee] = Payee.readsPayeeFromApi

    "return JsSuccess" when {
      "JSON is valid" in
        forAll(randomPayees) { expectedPayee =>
          val json        = getJsonFrom(expectedPayee)
          val actualPayee = json.validate[Payee].asEither.value
          actualPayee shouldBe expectedPayee
        }
    }
    "return JsError" when {
      "payee type is missing/invalid" in
        forAll(randomPayeeJsonWithPayeeTypeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("payee_type")
        }
      "payee type is CCP and their URN is missing/invalid" in
        forAll(randomCcpJsonWithUrnError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_reg_reference")
        }
      "payee type is CCP and their postcode is missing/invalid" in
        forAll(randomCcpJsonWithPostcodeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_postcode")
        }
    }
  }
  "val readsCcpFromApi"   should {
    implicit val reads: Reads[Payee] = Payee.readsCcpFromApi

    "return JsSuccess" when {
      "JSON is valid" in
        forAll(randomChildCareProviders) { expectedCcp =>
          val json      = getJsonFrom(expectedCcp)
          val actualCcp = json.validate[Payee].asEither.value
          actualCcp shouldBe expectedCcp
        }
    }
    "return JsError" when {
      "payee type is missing/invalid/not CCP" in
        forAll(randomCcpJsonWithPayeeTypeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("payee_type")
        }
      "CCP URN is missing/invalid" in
        forAll(randomCcpJsonWithUrnError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_reg_reference")
        }

      "CCP postcode is missing/invalid" in
        forAll(randomCcpJsonWithPostcodeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode  = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_postcode")
        }
    }
  }
}
