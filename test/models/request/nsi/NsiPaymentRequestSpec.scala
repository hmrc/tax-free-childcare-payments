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

package models.request.nsi

import helpers.BaseSpec
import models.request.external.validation.PayeeType
import play.api.libs.json.{JsNumber, JsString, Json}

class NsiPaymentRequestSpec extends BaseSpec {

  "writes" should {
    "format fields to the correct JSON object" in {
      val request = NsiPaymentRequest(
        eppAccount = "eppAcount",
        eppURN = "eppURN",
        amount = 1000,
        childAccountPaymentRef = "childAccountPaymentRef",
        payeeType = PayeeType.CCP,
        ccpURN = "ccpURN",
        ccpPostcode = "ccoPostcode"
      )

      Json.toJson(request) shouldBe Json.obj(
        "eppAccount"             -> JsString("eppAcount"),
        "eppURN"                 -> JsString("eppURN"),
        "amount"                 -> JsNumber(1000),
        "childAccountPaymentRef" -> JsString("childAccountPaymentRef"),
        "payeeType"              -> JsString("CCP"),
        "ccpURN"                 -> JsString("ccpURN"),
        "ccpPostcode"            -> JsString("ccoPostcode")
      )
    }
  }

}
