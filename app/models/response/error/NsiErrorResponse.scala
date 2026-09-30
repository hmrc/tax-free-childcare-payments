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

import play.api.http.Status.*

enum NsiErrorResponse(override val reportAsStatus: Int, override val message: String) extends ErrorResponse {

  case E0000
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0001
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0002
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0003
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0004
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0005
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0006
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0007
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0008
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0009
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0020 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")

  case E0021
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0022
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0023
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0024
      extends NsiErrorResponse(
        BAD_REQUEST,
        "Please check that the epp_reg_reference and epp_unique_customer_id are both correct"
      )

  case E0025
      extends NsiErrorResponse(
        BAD_REQUEST,
        "Please check that the child_date_of_birth and outbound_child_payment_reference are both correct"
      )

  case E0026 extends NsiErrorResponse(BAD_REQUEST, "Please check the outbound_child_payment_ref supplied")

  case E0027
      extends NsiErrorResponse(
        BAD_REQUEST,
        "The Childcare Provider (CCP) you have specified is not linked to the TFC Account. The parent must go into their TFC Portal and add the CCP to their account first before attempting payment again later."
      )

  case E0401
      extends NsiErrorResponse(
        INTERNAL_SERVER_ERROR,
        "We encountered an error on our servers and did not process your request, please try again later."
      )

  case E0030
      extends NsiErrorResponse(
        BAD_REQUEST,
        "The External Payment Provider (EPP) record is inactive on the TFC system. The EPP must complete the sign up process on the TFC Portal or contact their HMRC POC for further information."
      )

  case E0031
      extends NsiErrorResponse(
        BAD_REQUEST,
        "The CCP is inactive, please check the CCP details and ensure that the CCP is still registered with their childcare regulator and that they have also signed up to TFC via the TFC portal to receive TFC funds."
      )

  case E0032
      extends NsiErrorResponse(
        BAD_REQUEST,
        "The epp_unique_customer_id or epp_reg_reference is not associated with the outbound_child_payment_ref"
      )

  case E0033
      extends NsiErrorResponse(BAD_REQUEST, "The TFC account used to request payment contains insufficient funds.")

  case E0034 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")

  case E0035
      extends NsiErrorResponse(
        BAD_REQUEST,
        "There is an issue with this TFC Account, please advise parent / carer to contact TFC customer Services"
      )

  case E0036 extends NsiErrorResponse(BAD_REQUEST, "Error processing payment due to Payee bank details")

  case E0042
      extends NsiErrorResponse(
        BAD_REQUEST,
        "The ccp_reg_reference could not be found in the TFC system or does not correlate with the ccp_postcode. Please check the details and try again."
      )

  case E0043
      extends NsiErrorResponse(
        BAD_REQUEST,
        "Parent associated with the bearer token does not have a TFC account. The parent must create a TFC account."
      )

  case E9000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case E9999 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case E8000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case E8001 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")

}

object NsiErrorResponse {

  private[error] val byName: Map[String, NsiErrorResponse] =
    values.toSeq.map(response => response.toString.toLowerCase -> response).toMap

  def withName(name: String): Option[NsiErrorResponse] = byName.get(name.toLowerCase)

}
