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

package helpers.generators.response.external

import helpers.generators.other.NameGenerators
import models.response.external.ExternalLinkResponse
import org.scalacheck.Gen

trait ExternalLinkResponseGenerators extends NameGenerators {

  protected val genExternalLinkResponses: Gen[ExternalLinkResponse] = for {
    child_full_name <- genFullNames
  } yield ExternalLinkResponse(
    child_full_name = child_full_name
  )

}
