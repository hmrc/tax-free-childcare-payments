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

import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*

enum ServiceErrorResponse(override val reportAsStatus: Int, override val message: String) extends ErrorResponse {

  case E0000
      extends ServiceErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0001
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "outbound_child_payment_ref is in invalid format or missing"
      )

  case E0002
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "epp_reg_reference is in invalid format or missing"
      )

  case E0003
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "ccp_reg_reference is in invalid format or missing"
      )

  case E0004
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "epp_unique_customer_id is in invalid format or missing"
      )

  case E0006
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "child_date_of_birth is in invalid format or missing"
      )

  case E0007
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "payee_type is in invalid format or missing"
      )

  case E0008
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "payment_amount is in invalid format or missing"
      )

  case E0009
      extends ServiceErrorResponse(
        BAD_REQUEST,
        "ccp_postcode is in invalid format or missing"
      )

  case ETFC1 extends ServiceErrorResponse(BAD_REQUEST, "Correlation ID is in an invalid format or is missing")
  case ETFC2 extends ServiceErrorResponse(INTERNAL_SERVER_ERROR, "Bearer Token did not return a valid record")
  case ETFC3 extends ServiceErrorResponse(BAD_GATEWAY, "Bad Gateway") // Unexpected NSI response
  case ETFC4 extends ServiceErrorResponse(BAD_GATEWAY, "Bad Gateway") // Unexpected NSI errorCode
}

object ServiceErrorResponse extends Logging {

  def fromValidationError(error: JsError): ServiceErrorResponse =
    optionFromValidationError(error).getOrElse {
      logger.error(s"Unable to match JsError to to service error code. Returning E0000: $error")

      E0000
    }

  private def optionFromValidationError(error: JsError): Option[ServiceErrorResponse] =
    for {
      case (JsPath(KeyPathNode(key) :: Nil), _) <- error.errors.headOption
      errorResponse <- JSON_VALIDATION_ERROR_CODES.get(key)
    } yield errorResponse

  private val JSON_VALIDATION_ERROR_CODES: Map[String, ServiceErrorResponse] = Map(
    "outbound_child_payment_ref" -> ServiceErrorResponse.E0001,
    "epp_reg_reference"          -> ServiceErrorResponse.E0002,
    "ccp_reg_reference"          -> ServiceErrorResponse.E0003,
    "epp_unique_customer_id"     -> ServiceErrorResponse.E0004,
    "child_date_of_birth"        -> ServiceErrorResponse.E0006,
    "payee_type"                 -> ServiceErrorResponse.E0007,
    "payment_amount"             -> ServiceErrorResponse.E0008,
    "ccp_postcode"               -> ServiceErrorResponse.E0009
  )

}
