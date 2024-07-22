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

import models.requests.LinkRequest.CHILD_DOB_KEY
import models.requests.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}
import models.response.ErrorDescriptions
import play.api.libs.json._

object ErrorResponseJsonFactory extends ErrorDescriptions {

  // noinspection ScalaStyle
  def getJson(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): JsValue =
    errors.head match {
      case (JsPath(KeyPathNode(key) :: Nil), error :: _) =>
        (key, error.message) match {
          case (TFC_ACCOUNT_REF_KEY, "error.path.missing") => getJson("E0001", ERROR_400_DESCRIPTION)
          case (EPP_URN_KEY, "error.path.missing")         => getJson("E0002", ERROR_400_DESCRIPTION)
          case (EPP_ACCOUNT_ID_KEY, "error.path.missing")  => getJson("E0004", ERROR_400_DESCRIPTION)
          case (CHILD_DOB_KEY, "error.path.missing")       => getJson("E0006", ERROR_400_DESCRIPTION)
          case (CHILD_DOB_KEY, _)                          => getJson("E0021", ERROR_400_DESCRIPTION)
          case (TFC_ACCOUNT_REF_KEY, _)                    => getJson("E0000", ERROR_400_DESCRIPTION)
          case (EPP_URN_KEY, _)                            => getJson("E0000", ERROR_400_DESCRIPTION)
          case (EPP_ACCOUNT_ID_KEY, _)                     => getJson("E0000", ERROR_400_DESCRIPTION)
          case _                                           => getJson("BAD_REQUEST", "Request data is invalid or missing")
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
