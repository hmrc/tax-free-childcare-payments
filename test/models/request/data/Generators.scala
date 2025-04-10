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

package models.request.data

import models.request._

import play.api.mvc.Headers
import play.api.test.FakeRequest

trait Generators extends LinkRequestGenerators with PaymentRequestGenerators {

  import org.scalacheck.{Arbitrary, Gen}
  import Arbitrary.arbitrary

  protected implicit def arbIdentifierRequest[A: Arbitrary]: Arbitrary[IdentifierRequest[A]] = Arbitrary(
    randomIdentifierRequest(arbitrary[A])
  )

  protected def randomIdentifierRequest[A](randomBody: Gen[A]): Gen[IdentifierRequest[A]] = for {
    nino          <- randomNinos
    correlationId <- Gen.uuid
    body          <- randomBody
  } yield IdentifierRequest(nino, correlationId, FakeRequest("", "", Headers(), body))

}
