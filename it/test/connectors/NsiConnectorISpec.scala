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

import base.BaseISpec

class NsiConnectorISpec extends BaseISpec {
  private val connector = app.injector.instanceOf[NsiConnector]

  "method linkAccounts" should {
    s"respond $OK with a defined LinkResponse" when {
      s"NSI responds $CREATED with expected JSON format" in {

      }
    }
  }

  "method checkBalance" should {
    s"respond $OK with a defined BalanceResponse" when {
      s"NSI responds $OK with expected JSON format" in {

      }
    }
  }

  "method makePayment" should {
    s"respond $OK with a defined PaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in {

      }
    }
  }
}
