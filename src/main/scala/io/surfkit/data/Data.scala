package io.surfkit.data

import play.api.libs.json.Json

object Data {

  case class Anchor(text: String, href: String, title: String)
  implicit val anchorWrites = Json.writes[Anchor]
  implicit val anchorReads = Json.reads[Anchor]

  case class TopicAnchor( topic: String, url: String, links: List[Anchor])
  implicit val tanchorWrites = Json.writes[TopicAnchor]
  implicit val tanchorReads = Json.reads[TopicAnchor]

  case class EntityMeta(
                         uri:String,
                         rss: Set[String],
                         timestamp: String,
                         version:String,
                         icon: String,
                         thumb: String,
                         domain:String,
                         publishDate:Option[String],
                         contentType:String,
                         title:String,
                         description:String,
                         authors:Set[String],
                         keywords:Set[String],
                         coverUrl:String,
                         imgs:Set[String],
                         meta:Map[String, String],
                         content:Option[String],
                         raw:Option[String]
                       )

  implicit val metaWrites = Json.writes[EntityMeta]
  implicit val metaReads = Json.reads[EntityMeta]

  case class TopicEtl(topic: String, url: String, sites:List[EntityMeta])
  implicit val tmetaWrites = Json.writes[TopicEtl]
  implicit val tmetaReads = Json.reads[TopicEtl]

  case class Rss(site: String, rss:Set[String])
  implicit val rssWrites = Json.writes[Rss]
  implicit val rssReads = Json.reads[Rss]

  case class TopicRss(topic: String, url: String, sites:List[Rss])
  implicit val trssWrites = Json.writes[TopicRss]
  implicit val trssReads = Json.reads[TopicRss]
}
