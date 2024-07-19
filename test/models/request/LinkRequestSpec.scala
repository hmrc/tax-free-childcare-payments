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
import models.requests.LinkRequest
import org.scalatest.{EitherValues, LoneElement}

class LinkRequestSpec extends BaseSpec with models.request.Generators with EitherValues with LoneElement {
  "API JSON reader" should {
    "report missing child DoB" in
      forAll(randomLinkRequestJsonWithMissingChildDob) { json =>
        val (jsPath, errors) =
          json
            .validate[LinkRequest]
            .asEither
            .left
            .value
            .loneElement

        val errorKey   = jsPath.path.loneElement.toString
        val errorValue = errors.loneElement.message

        errorKey shouldBe "/child_date_of_birth"
        errorValue shouldBe "error.path.missing"
      }

    "report invalid child DoB" in
      forAll(randomLinkRequestJsonWithInvalidChildDob) { json =>
        val (jsPath, errors) =
          json
            .validate[LinkRequest]
            .asEither
            .left
            .value
            .loneElement

        val errorKey   = jsPath.path.loneElement.toString
        val errorValue = errors.loneElement.message

        errorKey shouldBe "/child_date_of_birth"
        errorValue shouldBe "error.expected.date.isoformat"
      }
  }
}
