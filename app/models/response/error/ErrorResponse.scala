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

package models.response.error

import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.mvc.Results.Status

trait ErrorResponse {

  val reportAsStatus: Int

  val errorCode: String = toString

  val message: String

  val toJson: JsObject =
    Json.obj(
      "errorCode"        -> errorCode,
      "errorDescription" -> message
    )

  val toResult: Result = new Status(reportAsStatus)(toJson)
}

object ErrorResponse extends Logging {
  type Response[A] = Either[ErrorResponse, A]

  given Reads[ErrorResponse] =
    (__ \ "errorCode")
      .read[String]
      .map(str =>
        NsiErrorResponse.withName(str).getOrElse {
          logger.error(s"Received unknown error code '$str' from NS&I. Returning service error ETFC4")

          ServiceErrorResponse.ETFC4
        }
      )

  given OWrites[ErrorResponse] = response => response.toJson
}
