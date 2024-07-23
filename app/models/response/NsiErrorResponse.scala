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

import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, Reads}

sealed abstract class NsiErrorResponse(val reportAs: Int, val message: String)

object NsiErrorResponse extends Status {
  type Maybe[A] = Either[NsiErrorResponse, A]

  case object E0000 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0001 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0002 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0003 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0004 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0005 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0006 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0007 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0008 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")

  case object E0020 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")
  case object E0021 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0022 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0023 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  case object E0024 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0025 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0026 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")

  case object E0401 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")

  case object E0030 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0031 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0032 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0033 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0034 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")
  case object E0035 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")

  case object E0040 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0041 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0042 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  case object E0043 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")

  case object E9000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")
  case object E9999 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")
  case object E8000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")
  case object E8001 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")

  private val values = Set(
    E0000,
    E0001,
    E0002,
    E0003,
    E0004,
    E0005,
    E0006,
    E0007,
    E0008,
    E0020,
    E0021,
    E0022,
    E0023,
    E0024,
    E0025,
    E0026,
    E0401,
    E0030,
    E0031,
    E0032,
    E0033,
    E0034,
    E0035,
    E0040,
    E0041,
    E0042,
    E0043,
    E9000,
    E9999,
    E8000,
    E8001
  )

  implicit val reads: Reads[NsiErrorResponse] = json =>
    for {
      str <- (json \ "errorCode").validate[String]
      err <- values.find(_.toString equalsIgnoreCase str) match {
               case None        => JsError(s"Invalid error string: $str")
               case Some(value) => JsSuccess(value)
             }
    } yield err
}
