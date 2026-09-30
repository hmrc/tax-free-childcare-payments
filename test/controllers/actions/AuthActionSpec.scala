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

package controllers.actions

import helpers.BaseSpec
import models.request.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{ActionBuilder, AnyContent, ControllerComponents, Request}

class AuthActionSpec extends BaseSpec with MockitoSugar with BeforeAndAfterEach {

  private val controllerComponents                    = mock[ControllerComponents]
  private val retrieveNinoAction                      = mock[RetrieveNinoAction]
  private val extractRequestCorrelationIdHeaderAction = mock[ExtractRequestCorrelationIdHeaderAction]
  private val addResponseCorrelationIdHeaderAction    = mock[AddResponseCorrelationIdHeaderAction]

  private val actionBuilder           = mock[ActionBuilder[Request, AnyContent]]
  private val ninoActionBuilder       = mock[ActionBuilder[NinoRequest, AnyContent]]
  private val identifierActionBuilder = mock[ActionBuilder[IdentifierRequest, AnyContent]]

  private val authActions = new AuthAction(
    controllerComponents = controllerComponents,
    retrieveNinoAction = retrieveNinoAction,
    extractRequestCorrelationIdHeaderAction = extractRequestCorrelationIdHeaderAction,
    addResponseCorrelationIdHeaderAction = addResponseCorrelationIdHeaderAction
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(controllerComponents)
    reset(retrieveNinoAction)
    reset(extractRequestCorrelationIdHeaderAction)
    reset(addResponseCorrelationIdHeaderAction)
  }

  "identify" should {
    "call RetrieveNinoAction, then ExtractRequestCorrelationIdHeaderAction, then AddResponseCorrelationIdHeaderAction" in {
      when(controllerComponents.actionBuilder).thenReturn(actionBuilder)
      when(actionBuilder.andThen[NinoRequest](any())).thenReturn(ninoActionBuilder)
      when(ninoActionBuilder.andThen[IdentifierRequest](any())).thenReturn(identifierActionBuilder)
      when(identifierActionBuilder.andThen[IdentifierRequest](any())).thenReturn(identifierActionBuilder)

      authActions.identify

      val inOrder = Mockito.inOrder(controllerComponents, actionBuilder, ninoActionBuilder, identifierActionBuilder)

      inOrder.verify(controllerComponents).actionBuilder
      inOrder.verify(actionBuilder).andThen(retrieveNinoAction)
      inOrder.verify(ninoActionBuilder).andThen(extractRequestCorrelationIdHeaderAction)
      inOrder.verify(identifierActionBuilder).andThen(addResponseCorrelationIdHeaderAction)
    }

  }

}
