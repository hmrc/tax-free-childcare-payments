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

package models.response.nsi

import models.response.external.ExternalAccountStatus
import play.api.libs.json.{JsError, JsString, JsSuccess, Reads}

enum NsiAccountStatus {
  case ACTIVE, BLOCKED

  def toExternalAccountStatus: ExternalAccountStatus = this match {
    case ACTIVE  => ExternalAccountStatus.ACTIVE
    case BLOCKED => ExternalAccountStatus.INACTIVE
  }

}

object NsiAccountStatus {

  private val byName: Map[String, NsiAccountStatus] = values.map(status => status.toString -> status).toMap

  given Reads[NsiAccountStatus] = {
    case JsString(name) =>
      byName.get(name) match {
        case Some(accountStatus) => JsSuccess(accountStatus)
        case None                => JsError("error.invalid.account_status")
      }
    case _ => JsError("error.expected.account_status.string")
  }

}
