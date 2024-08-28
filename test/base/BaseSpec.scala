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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import models.request.IdentifierRequest
import org.scalactic.Prettifier
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, EitherValues, LoneElement, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import play.api.libs.json._

class BaseSpec
    extends AnyWordSpec
    with should.Matchers
    with OptionValues
    with EitherValues
    with ScalaCheckPropertyChecks
    with LoneElement {

  implicit val prettifier: Prettifier = {
    case IdentifierRequest(_, _, underlying) => s"IdentifierRequest( ${underlying.body} )"
    case other                               => Prettifier.default(other)
  }

  protected def checkErrorJson(
      actualJson: => JsValue,
      expectedErrorCode: String,
      expectedErrorDescription: String
    ): Assertion = {
    actualJson \ "errorCode"        shouldBe JsDefined(JsString(expectedErrorCode))
    actualJson \ "errorDescription" shouldBe JsDefined(JsString(expectedErrorDescription))
  }

  protected def checkJsonError[A: Reads](expectedJsonPath: String, expectedMessage: String)(json: JsValue): Assertion =
    checkJsonError { (jsPath, jsError) =>
      jsPath.path.loneElement shouldBe KeyPathNode(expectedJsonPath)
      jsError.message         shouldBe expectedMessage
    }(json)

  protected def checkJsonError[A: Reads](check: (JsPath, JsonValidationError) => Assertion)(json: JsValue): Assertion = {
    val (jsPath, jsErrors) = json.validate[A].asEither.left.value.loneElement

    check(jsPath, jsErrors.loneElement)
  }

  protected def checkLoneLog(expectedLevel: Level, expectedMessage: String)(logs: List[ILoggingEvent]): Unit = {
    val log = logs.loneElement

    log.getLevel   shouldBe expectedLevel
    log.getMessage shouldBe expectedMessage
  }
}
