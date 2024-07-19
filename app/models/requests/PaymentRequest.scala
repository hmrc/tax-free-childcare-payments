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

package models.requests

import controllers.TaxFreeChildcarePaymentsController.{of, pattern}
import models.requests.Payee.ChildCareProvider

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{__, Reads}

final case class PaymentRequest(
    sharedRequestData: SharedRequestData,
    payment_amount: Int,
    payee: Payee
  )

object PaymentRequest {

  implicit val readsFromApi: Reads[PaymentRequest] = (
    of[SharedRequestData] ~
      (__ \ "payment_amount").read[Int] ~
      of[Payee]
  )(PaymentRequest.apply _)

  lazy private implicit val readsPayee: Reads[Payee] = (
    (__ \ "payee_type").read[String](CCP_ONLY) ~
      of[ChildCareProvider]
  )((_, ccp) => ccp)

  lazy private implicit val readsCcp: Reads[ChildCareProvider] = (
    (__ \ "ccp_reg_reference").read(CCP_REG) ~
      (__ \ "ccp_postcode").read(POST_CODE)
  )(ChildCareProvider.apply _)

  lazy private val POST_CODE = pattern("[a-zA-Z0-9]{2,4}\\s*[a-zA-Z0-9]{3}".r)
  lazy private val CCP_ONLY  = pattern("CCP".r)
  lazy private val CCP_REG   = pattern(".{1,20}".r)
}
