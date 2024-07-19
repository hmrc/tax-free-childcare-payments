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

package utils

import models.response.ErrorDescriptions

import play.api.libs.json._

object ErrorResponseJsonFactory extends ErrorDescriptions {

  def getJson(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): JsValue =
    errors.head match {
      case (JsPath(KeyPathNode(key) :: Nil), error :: _) =>
        (key, error.message) match {
          case ("child_date_of_birth", "error.path.missing") => getJson("E0006", ERROR_400_DESCRIPTION)
          case ("child_date_of_birth", _)                    => getJson("E0023", ERROR_400_DESCRIPTION)
          case _                                             => getJson("BAD_REQUEST", "Request data is invalid or missing")
        }
      case _                                             => getJson("BAD_REQUEST", "Request data is invalid or missing")
    }

  @inline
  def getJson(errorCode: String, errorDescription: String): JsValue =
    Json.obj(
      "errorCode"        -> errorCode,
      "errorDescription" -> errorDescription
    )
}
