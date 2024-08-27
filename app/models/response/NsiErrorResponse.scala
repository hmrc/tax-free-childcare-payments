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

import enumeratum._
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.{Reads, __}

sealed abstract class NsiErrorResponse(val reportAs: Int, val message: String) extends EnumEntry

object NsiErrorResponse extends Enum[NsiErrorResponse] with Status with Logging {
  type Maybe[A] = Either[NsiErrorResponse, A]

  case object E0000 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0001 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0002 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0003 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0004 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0005 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0006 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0007 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0008 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0009 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")

  case object E0020 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway")
  case object E0021 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0022 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0023 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")
  case object E0024 extends NsiErrorResponse(BAD_REQUEST, "Please check that the epp_reg_reference and epp_unique_customer_id are both correct")
  case object E0025 extends NsiErrorResponse(BAD_REQUEST, "Please check that the child_date_of_birth and outbound_child_payment_reference are both correct")
  case object E0026 extends NsiErrorResponse(BAD_REQUEST, "Please check the outbound_child_payment_ref supplied")
  case object E0027 extends NsiErrorResponse(BAD_REQUEST, "The CCP you have specified is not linked to the TFC Account. Please ensure that the parent goes into their TFC Portal and adds the CCP to their account first before attempting payment again later.")

  case object E0401 extends NsiErrorResponse(INTERNAL_SERVER_ERROR, "We encountered an error on our servers and did not process your request, please try again later.")

  case object E0030 extends NsiErrorResponse(BAD_REQUEST, "The External Payment Provider (EPP) record is inactive on the TFC system. Please ensure EPP completes sign up process on TFC Portal or contact HMRC POC for further information")
  case object E0031 extends NsiErrorResponse(BAD_REQUEST, "The CCP is inactive, please check the CCP details and ensure that the CCP is still registered with their childcare regulator and that they have also signed up to TFC via the TFC portal to receive TFC funds.")
  case object E0032 extends NsiErrorResponse(BAD_REQUEST, "The epp_unique_customer_id or epp_reg_reference is not associated with the outbound_child_payment_ref")
  case object E0033 extends NsiErrorResponse(BAD_REQUEST, "The TFC account used to request payment contains insufficient funds.")
  case object E0034 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case object E0035 extends NsiErrorResponse(BAD_REQUEST, "There is an issue with this TFC Account, please advise parent / carer to contact TFC customer Services")
  case object E0036 extends NsiErrorResponse(BAD_REQUEST, "Error processing payment due to Payee bank details")

  case object E0041 extends NsiErrorResponse(BAD_REQUEST, "The epp_reg_reference could not be found in the TFC system. Please check the details and try again.")
  case object E0042 extends NsiErrorResponse(BAD_REQUEST, "The ccp_reg_reference could not be found in the TFC system or does not correlate with the ccp_postcode. Please check the details and try again.")
  case object E0043 extends NsiErrorResponse(BAD_REQUEST, "Parent associated with the bearer token does not have a TFC account. Please ask the parent to create a TFC account first.")

  case object E9000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case object E9999 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case object E8000 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")
  case object E8001 extends NsiErrorResponse(SERVICE_UNAVAILABLE, "The service is currently unavailable.")

  case object ETFC3 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway") // Unexpected NSI response
  case object ETFC4 extends NsiErrorResponse(BAD_GATEWAY, "Bad Gateway") // Unexpected NSI errorCode

  /** This must be lazy to stop NPE thrown by JSON reader. */
  lazy val values: IndexedSeq[NsiErrorResponse] = findValues

  implicit val reads: Reads[NsiErrorResponse] =
    (__ \ "errorCode").read[String].map { str =>
      values.find(_.toString equalsIgnoreCase str) getOrElse ETFC4
    }
}
