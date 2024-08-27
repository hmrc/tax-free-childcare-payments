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

import models.request.Payee
import models.request.Payee.CCP_REG_MAX_LEN
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, JsString, Json}

trait PayeeGenerators extends base.Generators {

  protected val randomPayeeJsonWithPayeeTypeError: Gen[JsObject] = Gen.oneOf(
    randomPayeeJsonWithMissingPayeeType,
    randomPayeeJsonWithInvalidPayeeType
  )

  protected val randomCcpJsonWithPayeeTypeError: Gen[JsObject] = Gen.oneOf(
    randomPayeeJsonWithMissingPayeeType,
    randomPayeeJsonWithInvalidPayeeType,
    validEppJson
  )

  protected val randomCcpJsonWithUrnError: Gen[JsObject] = Gen.oneOf(
    randomCcpJsonWithMissingUrn,
    randomCcpJsonWithInvalidUrn
  )

  protected val randomCcpJsonWithPostcodeError: Gen[JsObject] = Gen.oneOf(
    randomCcpJsonWithMissingPostcode,
    randomCcpJsonWithInvalidPostcode
  )

  private lazy val randomPayeeJsonWithMissingPayeeType = validPayeeJson.map(_ - "payee_type")

  private lazy val randomPayeeJsonWithInvalidPayeeType = for {
    payeeJson <- validPayeeJson
    payeeType <- invalidPayeeTypes
  } yield payeeJson + ("payee_type" -> payeeType)

  private lazy val randomCcpJsonWithMissingUrn = validCcpJson.map(_ - "ccp_reg_reference")

  private lazy val randomCcpJsonWithInvalidUrn = for {
    ccpJson <- validCcpJson
    ccpUrn  <- invalidCcpUrns
  } yield ccpJson + ("ccp_reg_reference" -> ccpUrn)

  private lazy val randomCcpJsonWithMissingPostcode = validCcpJson.map(_ - "ccp_postcode")

  private lazy val randomCcpJsonWithInvalidPostcode = for {
    ccpJson  <- validCcpJson
    postcode <- invalidPostcodes
  } yield ccpJson + ("ccp_postcode" -> postcode)

  protected lazy val validPayeeJson: Gen[JsObject] = Gen.oneOf(validCcpJson, validEppJson)
  protected lazy val validCcpJson: Gen[JsObject]   = randomChildCareProviders map getJsonFrom
  protected lazy val validEppJson: Gen[JsObject]   = Gen const Json.obj("payee_type" -> "EPP")

  protected def getJsonFrom(payee: Payee): JsObject = payee match {
    case Payee.ExternalPaymentProvider          => Json.obj("payee_type" -> "EPP")
    case Payee.ChildCareProvider(urn, postcode) => Json.obj(
        "payee_type"        -> "CCP",
        "ccp_reg_reference" -> urn,
        "ccp_postcode"      -> postcode
      )
  }

  protected lazy val invalidPayeeTypes: Gen[JsString] = Gen.oneOf(
    Gen.oneOf("ccp", "epp"),
    Gen.numStr
  ) map JsString.apply

  protected lazy val invalidCcpUrns: Gen[JsString] = Gen.oneOf(
    Gen const "",
    oversizedCcpUrns
  ) map JsString.apply

  private lazy val oversizedCcpUrns = Gen
    .chooseNum(CCP_REG_MAX_LEN + 1, Byte.MaxValue)
    .flatMap(size => Gen.stringOfN(size, Gen.asciiPrintableChar))

  private lazy val invalidPostcodes = Gen.oneOf(
    Gen.alphaStr,
    Gen.numStr
  ) map JsString.apply
}
