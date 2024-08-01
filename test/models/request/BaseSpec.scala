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

import play.api.libs.json._

abstract class BaseSpec extends base.BaseSpec with Generators with EitherValues with LoneElement {

  protected def checkJsonError[A: Reads](expectedJsonPath: String, expectedMessage: String)(json: JsValue): Assertion =
    checkJsonError { (jsPath, jsError) =>
      jsPath.path.loneElement shouldBe KeyPathNode(expectedJsonPath)
      jsError.message shouldBe expectedMessage
    }(json)

  protected def checkJsonError[A: Reads](check: (JsPath, JsonValidationError) => Assertion)(json: JsValue): Assertion = {
    val (jsPath, jsErrors) = json.validate[A].asEither.left.value.loneElement

    check(jsPath, jsErrors.loneElement)
  }
}
