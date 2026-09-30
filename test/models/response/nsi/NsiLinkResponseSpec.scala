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

import helpers.BaseSpec
import models.response.external.ExternalLinkResponse
import play.api.libs.json.{JsString, JsSuccess, Json}

class NsiLinkResponseSpec extends BaseSpec {

  private val nsiLinkResponseJson = Json.obj(
    "childFullName" -> JsString("childFullName")
  )

  private val nsiLinkResponse = NsiLinkResponse(
    childFullName = "childFullName"
  )

  private val externalLinkResponse = ExternalLinkResponse(
    child_full_name = "childFullName"
  )

  "reads" should {
    "correctly read valid data" in {
      nsiLinkResponseJson.validate[NsiLinkResponse] shouldBe JsSuccess(nsiLinkResponse)
    }
  }

  "toExternalLinkResponse" should {
    "return the correct data" in {
      nsiLinkResponse.toExternalLinkResponse shouldBe externalLinkResponse
    }
  }

}
