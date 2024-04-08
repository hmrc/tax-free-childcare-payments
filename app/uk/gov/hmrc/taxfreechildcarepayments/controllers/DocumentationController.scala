package uk.gov.hmrc.taxfreechildcarepayments.controllers

import controllers.Assets
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class DocumentationController @Inject() (assets: Assets, cc: ControllerComponents) extends BackendController(cc) {

  def definition(): Action[AnyContent] = {
    assets.at("/public/api", "definition.json")
  }

  def specification(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }
}
