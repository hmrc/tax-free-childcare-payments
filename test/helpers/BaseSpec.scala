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

package helpers

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

import helpers.json.JsValueConversions
import models.request.IdentifierRequest
import org.scalactic.Prettifier
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, EitherValues, LoneElement, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.*

abstract class BaseSpec
    extends AnyWordSpec
    with should.Matchers
    with OptionValues
    with EitherValues
    with ScalaCheckPropertyChecks
    with JsValueConversions
    with LoneElement {

  /*
    header: ResponseHeader,
    body: HttpEntity,
    newSession: Option[Session] = None,
    newFlash: Option[Flash] = None,
    newCookies: Seq[Cookie] = Seq.empty,
    attrs: TypedMap = TypedMap.empty
   */

  given Prettifier = {
    case IdentifierRequest(_, _, underlying) => s"IdentifierRequest( ${underlying.body} )"
    case other                               => Prettifier.default(other)
  }

  protected def checkJsonValidationError[A: Reads](
      expectedJsonPath: String,
      expectedMessage: String,
      messageArgs: Any*
  )(
      json: ToJsValue
  ): Assertion = {
    val actualJson = toJsValue(json)

    val jsError = actualJson.validate[A].asInstanceOf[JsError]

    val path = if (expectedJsonPath.isEmpty) JsPath else JsPath \ expectedJsonPath

    jsError shouldBe JsError((path, JsonValidationError(expectedMessage, messageArgs*)))
  }

}
