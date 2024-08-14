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

package connectors.scenarios

import base.Generators
import models.request.{IdentifierRequest, Payee, PaymentRequest, SharedRequestData}
import models.response.PaymentResponse
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.time.LocalDate
import java.util.UUID

final case class NsiMakePayment201Scenario(
    correlationId: UUID,
    childAccountPaymentRef: String,
    epp_urn: String,
    eppAccount: String,
    parentNino: String,
    payee: Payee,
    amount: Int,
    expectedResponse: PaymentResponse
  ) {

  val expectedRequestJson: JsObject = Json.obj(
    "paymentReference" -> expectedResponse.payment_reference,
    "paymentDate"      -> expectedResponse.estimated_payment_date
  )

  val identifierRequest: IdentifierRequest[PaymentRequest] = {
    val sharedRequestData = SharedRequestData(eppAccount, epp_urn, childAccountPaymentRef)

    IdentifierRequest(
      parentNino,
      correlationId,
      FakeRequest("", "", Headers(), PaymentRequest(sharedRequestData, amount, payee))
    )
  }
}

object NsiMakePayment201Scenario extends Generators {

  implicit val arb: Arbitrary[NsiMakePayment201Scenario] = Arbitrary(
    for {
      correlationId          <- Gen.uuid
      childAccountPaymentRef <- nonEmptyAlphaNumStrings
      eppURN                 <- nonEmptyAlphaNumStrings
      eppAccount             <- nonEmptyAlphaNumStrings
      parentNino             <- randomNinos
      payee                  <- randomPayees
      amount                 <- Gen.chooseNum(0, Int.MaxValue)
      expectedResponse       <- paymentResponses
    } yield apply(
      correlationId,
      childAccountPaymentRef,
      eppURN,
      eppAccount,
      parentNino,
      payee,
      amount,
      expectedResponse
    )
  )

  private lazy val paymentResponses = for {
    paymentRef       <- nonEmptyAlphaNumStrings
    paymentDelayDays <- Gen.chooseNum(0, DAYS_IN_MONTH)
  } yield PaymentResponse(paymentRef, LocalDate.now() plusDays paymentDelayDays)

  private lazy val DAYS_IN_MONTH = 31
}
