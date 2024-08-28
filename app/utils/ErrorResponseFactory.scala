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

package utils

import models.request.LinkRequest.CHILD_DOB_KEY
import models.request.Payee.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY}
import models.request.PaymentRequest.PAYMENT_AMOUNT_KEY
import models.request.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}
import models.response.NsiErrorResponse

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status

object ErrorResponseFactory {

  def getJson(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): JsValue = {
    val (JsPath(KeyPathNode(key) :: Nil), _) = errors.head
    val errorCode                            = JSON_VALIDATION_ERROR_CODES(key)
    getJson(errorCode, s"$key is in invalid format or missing")
  }

  def getResult(nsiErrorResponse: NsiErrorResponse): Result =
    new Status(nsiErrorResponse.reportAs)(
      getJson(nsiErrorResponse.toString, nsiErrorResponse.message)
    )

  @inline
  def getJson(errorCode: String, errorDescription: String): JsValue =
    Json.obj(
      "errorCode"        -> errorCode,
      "errorDescription" -> errorDescription
    )

  private val JSON_VALIDATION_ERROR_CODES = Map(
    TFC_ACCOUNT_REF_KEY -> "E0001",
    EPP_URN_KEY         -> "E0002",
    EPP_ACCOUNT_ID_KEY  -> "E0004",
    CHILD_DOB_KEY       -> "E0006",
    PAYEE_TYPE_KEY      -> "E0007",
    CCP_URN_KEY         -> "E0003",
    CCP_POSTCODE_KEY    -> "E0009",
    PAYMENT_AMOUNT_KEY  -> "E0008"
  )
}
