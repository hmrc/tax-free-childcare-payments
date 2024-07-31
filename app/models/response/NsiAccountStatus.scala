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

import play.api.libs.json._

sealed abstract class NsiAccountStatus(val toApiString: String)

object NsiAccountStatus {
  case object ACTIVE  extends NsiAccountStatus("ACTIVE")
  case object BLOCKED extends NsiAccountStatus("INACTIVE")

  val values: Set[NsiAccountStatus] = Set(ACTIVE, BLOCKED)

  implicit val readsFromNsi: Reads[NsiAccountStatus] = {
    case JsString(value) => values.find(_.toString == value) match {
        case Some(accountStatus) => JsSuccess(accountStatus)
        case None                => JsError("error.invalid.account_status")
      }
    case _               => JsError("error.expected.string")
  }

  implicit val writesToApi: Writes[NsiAccountStatus] = status => JsString(status.toApiString)
}
