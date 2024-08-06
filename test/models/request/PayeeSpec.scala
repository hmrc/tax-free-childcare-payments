package models.request

import play.api.libs.json.{KeyPathNode, Reads}

class PayeeSpec extends BaseSpec with RandomPayeeJson {
  "val readsPayeeFromApi" should {
    implicit val reads: Reads[Payee] = Payee.readsPayeeFromApi

    "return JsSuccess" when {
      "JSON is valid" in
        forAll(payees) { expectedPayee =>
          val json = getJsonFrom(expectedPayee)
          val actualPayee = json.validate[Payee].asEither.value
          actualPayee shouldBe expectedPayee
        }
    }
    "return JsError" when {
      "payee type is missing/invalid" in
        forAll(randomPayeeJsonWithPayeeTypeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("payee_type")
        }
      "payee type is CCP and their URN is missing/invalid" in
        forAll(randomCcpJsonWithUrnError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_reg_reference")
        }
      "payee type is CCP and their postcode is missing/invalid" in
        forAll(randomCcpJsonWithPostcodeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_postcode")
        }
    }
  }
  "val readsCcpFromApi" should {
    implicit val reads: Reads[Payee] = Payee.readsCcpFromApi

    "return JsSuccess" when {
      "JSON is valid" in
        forAll(childCareProviders) { expectedCcp =>
          val json = getJsonFrom(expectedCcp)
          val actualCcp = json.validate[Payee].asEither.value
          actualCcp shouldBe expectedCcp
        }
    }
    "return JsError" when {
      "payee type is missing/invalid/not CCP" in
        forAll(randomCcpJsonWithPayeeTypeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("payee_type")
        }
      "CCP URN is missing/invalid" in
        forAll(randomCcpJsonWithUrnError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_reg_reference")
        }

      "CCP postcode is missing/invalid" in
        forAll(randomCcpJsonWithPostcodeError) { json =>
          val (jsonPath, _) = json.validate[Payee].asEither.left.value.loneElement
          val jsonPathNode = jsonPath.path.loneElement
          jsonPathNode shouldBe KeyPathNode("ccp_postcode")
        }
    }
  }
}
