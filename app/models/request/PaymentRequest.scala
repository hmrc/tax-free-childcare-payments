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

package models.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{ConstraintReads, Reads, __}

final case class PaymentRequest(
    sharedRequestData: SharedRequestData,
    payment_amount: Int,
    payee: Payee
  )

object PaymentRequest extends ConstraintReads {

  implicit def readsFromApi(implicit ofPayee: Reads[Payee]): Reads[PaymentRequest] = (
    of[SharedRequestData] ~
      (__ \ PAYMENT_AMOUNT_KEY).read(min(1)) ~
      ofPayee
  )(apply _)

  lazy val PAYMENT_AMOUNT_KEY = "payment_amount"
}
