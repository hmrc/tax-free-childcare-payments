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

import play.api.http.{Status => StatusCodes}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Status

sealed abstract class TfcErrorResponse2(status: Int, apiMessage: String, val logMessage: String) {

  def toResult: Result = new Status(status)(toJson)

  def toJson: JsObject = Json.obj(
    "errorCode"        -> toString,
    "errorDescription" -> apiMessage
  )
}

object TfcErrorResponse2 extends StatusCodes {
  case object ETFC1 extends TfcErrorResponse2(BAD_REQUEST, ERROR_400_DESCRIPTION, "Correlation-ID header is invalid or missing")
  case object ETFC2 extends TfcErrorResponse2(INTERNAL_SERVER_ERROR, ERROR_500_DESCRIPTION, "Unable to retrieve NI number")

  private lazy val ERROR_400_DESCRIPTION =
    "Request data is invalid or missing. Please refer to API Documentation for further information"

  private lazy val ERROR_500_DESCRIPTION =
    "The server encountered an error and couldn't process the request. Please refer to API Documentation for further information"
}
