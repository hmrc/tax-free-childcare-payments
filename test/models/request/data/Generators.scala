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
import play.api.libs.json._
import play.api.mvc.Headers
import play.api.test.FakeRequest

import java.time.{LocalDate, ZoneId}

trait Generators extends RandomPaymentRequestData {
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

  /** START Link Accounts generators */

  protected implicit val arbLinkRequest: Arbitrary[LinkRequest] = Arbitrary(
    for {
      sharedRequestData <- validSharedDataModels
      calendar          <- Gen.calendar
      childDateOfBirth   = calendar.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
      childYearOfBirth  <- Gen.chooseNum(2000, 3000)
    } yield LinkRequest(
      sharedRequestData,
      childDateOfBirth withYear childYearOfBirth
    )
  )

  protected def getJsonFrom(linkRequest: LinkRequest): JsObject =
    getJsonFrom(linkRequest.sharedRequestData) ++ Json.obj(
      "child_date_of_birth" -> linkRequest.child_date_of_birth
    )

  protected val validLinkPayloads: Gen[JsObject] = linkPayloadsWith(validSharedJson)

  protected lazy val linkPayloadsWithMissingTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingTfcAccountRef)

  protected lazy val linkPayloadsWithInvalidTfcAccountRef: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidTfcAccountRef)

  protected lazy val linkPayloadsWithMissingEppUrn: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingEppUrn)

  protected lazy val linkPayloadsWithInvalidEppUrn: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidEppUrn)

  protected lazy val linkPayloadsWithMissingEppAccountId: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithMissingEppAccountId)

  protected lazy val linkPayloadsWithInvalidEppAccountId: Gen[JsObject] = linkPayloadsWith(sharedPayloadsWithInvalidEppAccountId)

  protected lazy val linkPayloadsWithMissingChildDob: Gen[JsValue] = validSharedJson

  protected lazy val linkPayloadsWithNonStringChildDob: Gen[JsObject] = for {
    sharedPayload  <- validSharedJson
    nonStringValue <- Gen.long map { num => JsNumber(num) }
  } yield sharedPayload + ("child_date_of_birth" -> nonStringValue)

  protected lazy val linkPayloadsWithNonIso8061ChildDob: Gen[JsObject] = for {
    sharedPayload   <- validSharedJson
    nonIso8061Value <- Gen.alphaNumStr map JsString.apply
  } yield sharedPayload + ("child_date_of_birth" -> nonIso8061Value)

  private def linkPayloadsWith(sharedPayloads: Gen[JsObject]) =
    for {
      sharedPayload <- sharedPayloads
      childAgeDays  <- Gen.chooseNum(0, 18 * 365)
    } yield sharedPayload ++ Json.obj(
      "child_date_of_birth" -> (LocalDate.now() minusDays childAgeDays)
    )
}
