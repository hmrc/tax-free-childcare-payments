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

trait ErrorDescriptions extends Status {

  protected val ERROR_400_DESCRIPTION =
    "Request data is invalid or missing. Please refer to API Documentation for further information"

  protected val ERROR_500_DESCRIPTION =
    "The server encountered an error and couldn't process the request. Please refer to API Documentation for further information"

  val descriptions: Map[Int, String] = Map(
    BAD_REQUEST           -> ERROR_400_DESCRIPTION,
    INTERNAL_SERVER_ERROR -> ERROR_500_DESCRIPTION
  )
}
