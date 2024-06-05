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

  private case object E0000 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0001 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0002 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0003 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0004 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0005 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0006 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")
  private case object E0007 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "The server encountered an error and couldn't process the request")
  private case object E0008 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  private case object E0009 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  private case object E0010 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  private case object E0020 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  private case object E0021 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")
  private case object E0022 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")
  private case object E0024 extends NsiErrorResponse(BAD_REQUEST, "Request data is invalid or missing")

  private case object E9000 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")
  private case object E9999 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")

  private case object E8000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")
  private case object E8001 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable")

  private val values = Set(E0000, E0001, E0002, E0003, E0004, E0005, E0006, E0007, E0008, E0009, E0010, E0020, E0021, E0022, E0024, E9000, E9999, E8000, E8001)

  implicit val reads: Reads[NsiErrorResponse] = json =>
    for {
      str <- (json \ "errorCode").validate[String]
      err <- values.find(_.toString equalsIgnoreCase str) match {
               case None        => JsError(s"Invalid error string: $str")
               case Some(value) => JsSuccess(value)
             }
    } yield err
}
