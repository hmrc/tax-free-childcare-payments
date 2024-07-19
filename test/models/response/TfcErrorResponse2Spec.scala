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

import base.BaseSpec
import models.response.TfcErrorResponse2._
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status

class TfcErrorResponse2Spec extends BaseSpec with TableDrivenPropertyChecks with Status {
  "method toJson" should {
    "convey data within model" in
      forAll(errorScenarios) {
        (actualErrorResponse, _, expectedErrorCode, expectedErrorDescription) =>
          checkErrorJson(actualErrorResponse.toJson, expectedErrorCode, expectedErrorDescription)
      }
  }

  private lazy val errorScenarios = Table(
    ("Error Response", "Expected Status", "Expected Error Code", "Expected Error Description"),
    (ETFC1, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION),
    (ETFC2, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_500_ERROR_DESCRIPTION),
    (E0006, BAD_REQUEST, "E0006", EXPECTED_400_ERROR_DESCRIPTION),
    (E0023, INTERNAL_SERVER_ERROR, "E0023", EXPECTED_400_ERROR_DESCRIPTION)
  )
}
