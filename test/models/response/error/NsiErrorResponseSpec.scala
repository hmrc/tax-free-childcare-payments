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

package models.response.error

import helpers.BaseSpec
import helpers.error.ExpectedErrorResponses
import models.response.error.NsiErrorResponse.*
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json

class NsiErrorResponseSpec extends BaseSpec with ExpectedErrorResponses {

  private val nsiErrorScenarios = Table(
    ("Error", "Expected Status", "Expected Error Code", "Expected Error Description"),
    (E0000, INTERNAL_SERVER_ERROR, "E0000", EXPECTED_500_DESC),
    (E0001, INTERNAL_SERVER_ERROR, "E0001", EXPECTED_500_DESC),
    (E0002, INTERNAL_SERVER_ERROR, "E0002", EXPECTED_500_DESC),
    (E0003, INTERNAL_SERVER_ERROR, "E0003", EXPECTED_500_DESC),
    (E0004, INTERNAL_SERVER_ERROR, "E0004", EXPECTED_500_DESC),
    (E0005, INTERNAL_SERVER_ERROR, "E0005", EXPECTED_500_DESC),
    (E0006, INTERNAL_SERVER_ERROR, "E0006", EXPECTED_500_DESC),
    (E0007, INTERNAL_SERVER_ERROR, "E0007", EXPECTED_500_DESC),
    (E0008, INTERNAL_SERVER_ERROR, "E0008", EXPECTED_500_DESC),
    (E0009, INTERNAL_SERVER_ERROR, "E0009", EXPECTED_500_DESC),
    (E0020, BAD_GATEWAY, "E0020", EXPECTED_502_DESC),
    (E0021, INTERNAL_SERVER_ERROR, "E0021", EXPECTED_500_DESC),
    (E0022, INTERNAL_SERVER_ERROR, "E0022", EXPECTED_500_DESC),
    (E0023, INTERNAL_SERVER_ERROR, "E0023", EXPECTED_500_DESC),
    (E0024, BAD_REQUEST, "E0024", EXPECTED_E0024_DESC),
    (E0025, BAD_REQUEST, "E0025", EXPECTED_E0025_DESC),
    (E0026, BAD_REQUEST, "E0026", EXPECTED_E0026_DESC),
    (E0027, BAD_REQUEST, "E0027", EXPECTED_E0027_DESC),
    (E0401, INTERNAL_SERVER_ERROR, "E0401", EXPECTED_500_DESC),
    (E0030, BAD_REQUEST, "E0030", EXPECTED_E0030_DESC),
    (E0031, BAD_REQUEST, "E0031", EXPECTED_E0031_DESC),
    (E0032, BAD_REQUEST, "E0032", EXPECTED_E0032_DESC),
    (E0033, BAD_REQUEST, "E0033", EXPECTED_E0033_DESC),
    (E0034, SERVICE_UNAVAILABLE, "E0034", EXPECTED_503_DESC),
    (E0035, BAD_REQUEST, "E0035", EXPECTED_E0035_DESC),
    (E0036, BAD_REQUEST, "E0036", EXPECTED_E0036_DESC),
    (E0042, BAD_REQUEST, "E0042", EXPECTED_E0042_DESC),
    (E0043, BAD_REQUEST, "E0043", EXPECTED_E0043_DESC),
    (E9000, SERVICE_UNAVAILABLE, "E9000", EXPECTED_503_DESC),
    (E9999, SERVICE_UNAVAILABLE, "E9999", EXPECTED_503_DESC),
    (E8000, SERVICE_UNAVAILABLE, "E8000", EXPECTED_503_DESC),
    (E8001, SERVICE_UNAVAILABLE, "E8001", EXPECTED_503_DESC)
  )

  "values" should {
    "contain all expected values" in {
      NsiErrorResponse.values shouldBe nsiErrorScenarios.map(_._1)
    }
  }

  "writes" should
    nsiErrorScenarios.foreach { case (errorResponse, _, errorCode, errorDescription) =>
      s"format error response $errorResponse to json object with errorCode of '$errorCode' and description '$errorDescription'" in {
        Json.toJson[ErrorResponse](errorResponse) shouldBe Json.obj(
          "errorCode"        -> errorCode,
          "errorDescription" -> errorDescription
        )
      }
    }

  "toResult" should
    nsiErrorScenarios.foreach { case (errorResponse, statusCode, _, _) =>
      s"return HTTP status code $statusCode for error response $errorResponse" in {
        errorResponse.toResult.header.status shouldBe statusCode
      }
    }

}
