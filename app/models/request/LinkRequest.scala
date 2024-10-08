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

package models.request

import java.time.LocalDate
import scala.util.Try

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsResult, Reads, __}

final case class LinkRequest(
    sharedRequestData: SharedRequestData,
    child_date_of_birth: LocalDate
  )

object LinkRequest {

  implicit val readsFromApi: Reads[LinkRequest] = (
    __.read[SharedRequestData] ~
      (__ \ CHILD_DOB_KEY)
        .read[String]
        .flatMapResult(str => JsResult fromTry Try(LocalDate parse str))
  )(apply _)

  lazy val CHILD_DOB_KEY = "child_date_of_birth"
}
