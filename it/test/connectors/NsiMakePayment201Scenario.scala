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

package connectors

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{created, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.requests.PaymentRequest.PayeeType
import models.requests.{IdentifierRequest, PaymentRequest, SharedRequestData}
import models.response.PaymentResponse
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.Json
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
    opt_ccp_urn: Option[String],
    amount: Int,
    expectedResponse: PaymentResponse
  ) {
  private val payeeType = if (opt_ccp_urn.isDefined) PayeeType.CCP else PayeeType.EPP

  def stubNsiResponse(endpoint: MappingBuilder): StubMapping = stubFor {
    val body = Json.obj(
      "paymentReference" -> expectedResponse.payment_reference,
      "paymentDate"      -> expectedResponse.estimated_payment_date
    )

    endpoint willReturn created().withBody(body.toString)
  }

  val identifierRequest: IdentifierRequest[PaymentRequest] = {
    val sharedRequestData = SharedRequestData(eppAccount, epp_urn, childAccountPaymentRef)

    IdentifierRequest(
      parentNino,
      correlationId,
      FakeRequest("", "", Headers(), PaymentRequest(sharedRequestData, amount, opt_ccp_urn getOrElse "", "", payeeType))
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
      parentNino             <- ninos
      opt_ccp_urn            <- Gen option nonEmptyAlphaNumStrings
      amount                 <- Gen.chooseNum(0, Int.MaxValue)
      expectedResponse       <- paymentResponses
    } yield apply(
      correlationId,
      childAccountPaymentRef,
      eppURN,
      eppAccount,
      parentNino,
      opt_ccp_urn,
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
