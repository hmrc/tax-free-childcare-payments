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

import org.scalatest.{Assertion, EitherValues, LoneElement}

import play.api.libs.json.{JsValue, KeyPathNode, Reads}

abstract class BaseSpec extends base.BaseSpec with Generators with EitherValues with LoneElement {

  protected def checkJsonError[A: Reads](expectedJsonPath: String, expectedMessage: String)(invalidJson: JsValue): Assertion = {
    val (jsPath, jsErrors) = invalidJson.validate[A].asEither.left.value.loneElement

    jsPath.path.loneElement shouldBe KeyPathNode(expectedJsonPath)
    jsErrors.loneElement.message shouldBe expectedMessage
  }
}
