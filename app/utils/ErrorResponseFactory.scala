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

import models.requests.LinkRequest.CHILD_DOB_KEY
import models.requests.Payee.{CCP_POSTCODE_KEY, CCP_URN_KEY, PAYEE_TYPE_KEY}
import models.requests.PaymentRequest.PAYMENT_AMOUNT_KEY
import models.requests.SharedRequestData.{EPP_ACCOUNT_ID_KEY, EPP_URN_KEY, TFC_ACCOUNT_REF_KEY}
import models.response.{ErrorDescriptions, NsiErrorResponse}

import play.api.libs.json._
import play.api.mvc.Results.Status
import play.api.mvc.{RequestHeader, Result}

object ErrorResponseFactory extends ErrorDescriptions with FormattedLogging {

  // noinspection ScalaStyle
  def getJson(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): JsValue = {
    val errorCode = errors.head match {
      case (JsPath(KeyPathNode(key) :: Nil), error :: _) =>
        (key, error.message) match {
          case (TFC_ACCOUNT_REF_KEY, "error.path.missing") => "E0001"
          case (EPP_URN_KEY, "error.path.missing")         => "E0002"
          case (CCP_URN_KEY, "error.path.missing")         => "E0003"
          case (EPP_ACCOUNT_ID_KEY, "error.path.missing")  => "E0004"
          case (CHILD_DOB_KEY, "error.path.missing")       => "E0006"
          case (PAYEE_TYPE_KEY, "error.path.missing")      => "E0007"
          case (PAYMENT_AMOUNT_KEY, "error.path.missing")  => "E0008"
          case (CHILD_DOB_KEY, _)                          => "E0021"
          case (PAYEE_TYPE_KEY, _)                         => "E0022"
          case (PAYMENT_AMOUNT_KEY, _)                     => "E0023"
          case (TFC_ACCOUNT_REF_KEY, _)                    => "E0000"
          case (EPP_URN_KEY, _)                            => "E0000"
          case (EPP_ACCOUNT_ID_KEY, _)                     => "E0000"
          case (CCP_URN_KEY, _)                            => "E0000"
          case (CCP_POSTCODE_KEY, _)                       => "E0000"
          case _                                           => "E0000"
        }
      case _                                             => "E0000"
    }

    getJson(errorCode, ERROR_400_DESCRIPTION)
  }

  def getResult(nsiErrorResponse: NsiErrorResponse)(implicit req: RequestHeader): Result = {
    val errorCode = nsiErrorResponse.toString
    val errorDesc = nsiErrorResponse.message
    val logMsg    = formattedLog(s"$errorCode - $errorDesc")

    if (nsiErrorResponse.reportAs < 500) {
      logger.info(logMsg)
    } else {
      logger.warn(logMsg)
    }

    new Status(nsiErrorResponse.reportAs)(
      getJson(errorCode, descriptions(nsiErrorResponse.reportAs))
    )
  }

  @inline
  def getJson(errorCode: String, errorDescription: String): JsValue =
    Json.obj(
      "errorCode"        -> errorCode,
      "errorDescription" -> errorDescription
    )
}
