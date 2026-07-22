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
import models.request.Payee.ChildCareProvider.Urn.CCP_REG_MAX_LEN
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, JsString, Json}

trait PayeeGenerators extends base.Generators {

  protected val validCcpJson: Gen[JsObject]   = randomChildCareProviders.map(getJsonFrom)
  protected val validEppJson: Gen[JsObject]   = Gen.const(Json.obj("payee_type" -> "EPP"))
  protected val validPayeeJson: Gen[JsObject] = Gen.oneOf(validCcpJson, validEppJson)

  protected def getJsonFrom(payee: Payee): JsObject = payee match {
    case Payee.ExternalPaymentProvider => Json.obj("payee_type" -> "EPP")
    case Payee.ChildCareProvider(urn, postcode) =>
      Json.obj(
        "payee_type"        -> "CCP",
        "ccp_reg_reference" -> urn,
        "ccp_postcode"      -> postcode
      )
  }

  protected val invalidPayeeTypes: Gen[JsString] = Gen
    .oneOf(
      Gen.oneOf("ccp", "epp"),
      Gen.numStr
    )
    .map(JsString.apply)

  private val oversizedCcpUrns = Gen
    .chooseNum(CCP_REG_MAX_LEN + 1, Byte.MaxValue)
    .flatMap(size => Gen.stringOfN(size, Gen.asciiPrintableChar))

  protected val invalidCcpUrns: Gen[JsString] = Gen
    .oneOf(
      Gen.const(""),
      oversizedCcpUrns
    )
    .map(JsString.apply)

  private val invalidPostcodes = Gen
    .oneOf(
      Gen.alphaStr,
      Gen.numStr
    )
    .map(JsString.apply)

  private val randomPayeeJsonWithMissingPayeeType: Gen[JsObject] = validPayeeJson.map(_ - "payee_type")

  private val randomPayeeJsonWithInvalidPayeeType: Gen[JsObject] = for {
    payeeJson <- validPayeeJson
    payeeType <- invalidPayeeTypes
  } yield payeeJson + ("payee_type" -> payeeType)

  private val randomCcpJsonWithMissingUrn: Gen[JsObject] = validCcpJson.map(_ - "ccp_reg_reference")

  private val randomCcpJsonWithInvalidUrn: Gen[JsObject] = for {
    ccpJson <- validCcpJson
    ccpUrn  <- invalidCcpUrns
  } yield ccpJson + ("ccp_reg_reference" -> ccpUrn)

  private val randomCcpJsonWithMissingPostcode: Gen[JsObject] = validCcpJson.map(_ - "ccp_postcode")

  private val randomCcpJsonWithInvalidPostcode: Gen[JsObject] = for {
    ccpJson  <- validCcpJson
    postcode <- invalidPostcodes
  } yield ccpJson + ("ccp_postcode" -> postcode)

  val randomPayeeJsonWithPayeeTypeError: Gen[JsObject] = Gen.oneOf(
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

}
