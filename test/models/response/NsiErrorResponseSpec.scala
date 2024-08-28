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

import play.api.libs.json.Json

class NsiErrorResponseSpec extends BaseSpec {
  "JSON reader" should {
    "not throw a NullPointerException" when {
      "given an unknown errorCode" in {
        val json = Json.obj("errorCode" -> "UNKNOWN", "errorDescription" -> "--missing-error-description--")

        /** This statement is required to reproduce the [[NullPointerException]]. */
        val _ = NsiErrorResponse.E0000

        assert(json.validate[NsiErrorResponse].isSuccess)
      }
    }
  }
}
