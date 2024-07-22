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
import models.requests.SharedRequestData
import org.scalatest.{EitherValues, LoneElement}

import play.api.libs.json.{JsPath, KeyPathNode}

class SharedRequestDataSpec extends BaseSpec with Generators with EitherValues with LoneElement {

  "API JSON reader" should {
    "return JsSuccess" when {
      "given valid input JSON" in
        forAll(validSharedDataModels) { expectedSharedDataModel =>
          val validJson = getJsonFrom(expectedSharedDataModel)

          val actualSharedDataModel = validJson.validate[SharedRequestData].asEither.value

          actualSharedDataModel shouldBe expectedSharedDataModel
        }
    }

    "return JsError" when {
      "field outbound_child_payment_ref is missing" in
        forAll(sharedDataPayloadsWithMissingTfcAccountRef) { invalidJson =>
          val (jsPath, jsErrors) = invalidJson.validate[SharedRequestData].asEither.left.value.loneElement

          jsPath shouldBe JsPath(List(KeyPathNode("outbound_child_payment_ref")))
          jsErrors.loneElement.message shouldBe "error.path.missing"
        }

      "field outbound_child_payment_ref is invalid" in
        forAll(sharedDataPayloadsWithInvalidTfcAccountRef) { invalidJson =>
          val (jsPath, jsErrors) = invalidJson.validate[SharedRequestData].asEither.left.value.loneElement

          jsPath shouldBe JsPath(List(KeyPathNode("outbound_child_payment_ref")))
          jsErrors.loneElement.message shouldBe "error.pattern"
        }
    }
  }
}
