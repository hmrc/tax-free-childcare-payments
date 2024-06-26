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

package models.requests

import java.time.LocalDate

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{__, OFormat}

final case class LinkRequest(
    metadata: SharedRequestData,
    child_date_of_birth: LocalDate
  )

object LinkRequest {

  implicit val format: OFormat[LinkRequest] = (
    __.format[SharedRequestData] ~
      (__ \ "child_date_of_birth").format[LocalDate]
  )(apply, unlift(unapply))
}
