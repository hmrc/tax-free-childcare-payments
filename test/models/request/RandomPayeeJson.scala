package models.request

import models.request.Payee.CCP_REG_MAX_LEN
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, JsString, Json}

trait RandomPayeeJson extends base.Generators {

  protected val randomPayeeJsonWithPayeeTypeError: Gen[JsObject] = Gen.oneOf(
    randomPayeeJsonWithMissingPayeeType, randomPayeeJsonWithInvalidPayeeType
  )

  protected val randomCcpJsonWithPayeeTypeError: Gen[JsObject] = Gen.oneOf(
    randomPayeeJsonWithMissingPayeeType,
    randomPayeeJsonWithInvalidPayeeType,
    validEppJson
  )

  protected val randomCcpJsonWithUrnError: Gen[JsObject] = Gen.oneOf(
    randomCcpJsonWithMissingUrn, randomCcpJsonWithInvalidUrn
  )

  protected val randomCcpJsonWithPostcodeError: Gen[JsObject] = Gen.oneOf(
    randomCcpJsonWithMissingPostcode, randomCcpJsonWithInvalidPostcode
  )

  private lazy  val randomPayeeJsonWithMissingPayeeType = validPayeeJson.map(_ - "payee_type")

  private lazy val randomPayeeJsonWithInvalidPayeeType = for {
    payeeJson <- validPayeeJson
    payeeType <- invalidPayeeTypes
  } yield payeeJson + ("payee_type" -> payeeType)

  private lazy val randomCcpJsonWithMissingUrn = validCcpJson.map(_ - "ccp_reg_reference")

  private lazy val randomCcpJsonWithInvalidUrn = for {
    ccpJson <- validCcpJson
    ccpUrn <- invalidCcpUrns
  } yield ccpJson + ("ccp_reg_reference" -> ccpUrn)

  private lazy val randomCcpJsonWithMissingPostcode = validCcpJson.map(_ - "ccp_postcode")

  private lazy val randomCcpJsonWithInvalidPostcode = for {
    ccpJson <- validCcpJson
    postcode <- invalidPostcodes
  } yield ccpJson + ("ccp_postcode" -> postcode)

  protected lazy val validPayeeJson: Gen[JsObject] = Gen.oneOf(validCcpJson, validEppJson)
  protected lazy val validCcpJson: Gen[JsObject] = childCareProviders map getJsonFrom
  protected lazy val validEppJson: Gen[JsObject] = Gen const Json.obj("payee_type" -> "EPP")

  protected def getJsonFrom(payee: Payee): JsObject = payee match {
    case Payee.ExternalPaymentProvider => Json.obj("payee_type" -> "EPP")
    case ccp: Payee.ChildCareProvider => getJsonFrom(ccp)
  }

  protected def getJsonFrom(ccp: Payee.ChildCareProvider): JsObject = Json.obj(
    "payee_type"        -> "CCP",
    "ccp_reg_reference" -> ccp.urn,
    "ccp_postcode"      -> ccp.postcode
  )

  private lazy val invalidPayeeTypes = Gen.oneOf(
    Gen.oneOf("ccp", "epp"),
    Gen.numStr
  ) map JsString.apply

  private lazy val invalidCcpUrns = Gen.oneOf(
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
