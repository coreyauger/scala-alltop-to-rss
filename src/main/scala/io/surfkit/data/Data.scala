package io.surfkit.data

import play.api.libs.json.Json

object Data {

  case class Anchor(text: String, href: String, title: String)

  implicit val anchorWrites = Json.writes[Anchor]
  implicit val anchorReads = Json.reads[Anchor]

}
