/*
 * Copyright 2026 HM Revenue & Customs
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

package helpers.generators

import helpers.generators.other.NinoGenerators
import helpers.generators.request.external.{
  ExternalBalanceRequestGenerators,
  ExternalLinkRequestGenerators,
  ExternalPaymentRequestGenerators
}
import models.request.IdentifierRequest
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import org.scalacheck.Gen
import play.api.mvc.Headers
import play.api.test.FakeRequest

trait IdentifierRequestGenerators
    extends NinoGenerators
    with ExternalLinkRequestGenerators
    with ExternalBalanceRequestGenerators
    with ExternalPaymentRequestGenerators {

  protected def genIdentifierRequests[A](randomBody: Gen[A]): Gen[IdentifierRequest[A]] = for {
    nino          <- genNinos
    correlationId <- Gen.uuid
    body          <- randomBody
  } yield IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), body))

  protected val genLinkIdentifierRequests: Gen[IdentifierRequest[ExternalLinkRequest]] = genIdentifierRequests(
    genExternalLinkRequests
  )

  protected val genBalanceIdentifierRequests: Gen[IdentifierRequest[ExternalBalanceRequest]] = genIdentifierRequests(
    genExternalBalanceRequests
  )

  protected val genPaymentIdentifierRequests: Gen[IdentifierRequest[ExternalPaymentRequest]] = genIdentifierRequests(
    genExternalPaymentRequests
  )

}
