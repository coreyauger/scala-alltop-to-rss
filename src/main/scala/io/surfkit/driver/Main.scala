package io.surfkit.driver

import java.io.{FileOutputStream, File, PrintWriter}
import java.net.{URLEncoder, URI}
import java.nio.file.{FileSystems, Path, Files}
import javax.ws.rs.core.UriBuilder

import com.gravity.goose.{Goose, Configuration}
import io.surfkit.data.Data._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.libs.ws.ning._
import play.api.libs.ws._
import play.api.libs.json._
import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try, Failure}

/**
 *
 * Created by Corey Auger
 */

object Main extends App{

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
    f.map(Success(_)).recover({case e => Failure(e)})

  /*def extractor[T](urls: List[String], select: String)(f: (Element) => T):Future[List[T]] = {
    val ws = {
      val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
      new play.api.libs.ws.ning.NingWSClient(builder.build())
    }
    Future.sequence(urls.map{ url =>
      println(url)
      val futureList:Future[Try[List[T]]] =
        ws.url(url).withFollowRedirects(true).withRequestTimeout(5 * 60 * 1000).get.map { req =>  // 5 min timeout
          val jsoup = Jsoup.parse(req.body)
          jsoup.select(select).iterator().map(f).toList
      }.map(Success(_)).recover({case e => Failure(e)})
      futureList
    }).map(_.filter(_.isSuccess).map(_.get).flatten)
  }*/

  def extractor[T](urls: List[String])(f: (String, WSResponse) => List[T]):Future[List[T]] = {
    val ws = {
      val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
      new play.api.libs.ws.ning.NingWSClient(builder.build())
    }
    Future.sequence(urls.map(u => Try(URI.create(u)).toOption).filter(_ != None).map(_.get.toString).map{ url =>
      println(url)
      val futureList:Future[Try[List[T]]] =
        ws.url(url).withFollowRedirects(true).withRequestTimeout(15 * 1000).get.map { res =>  // 15 sec
          f(url, res)
        }.map(Success(_)).recover({case e => Failure(e)})
      futureList
    }).map(_.filter(_.isSuccess).map(_.get).flatten)
  }


  def extractIndex() = {
    // alpha range
    val alpha = 'a' to 'z'

    val urls = alpha.toList.map(l => s"http://alltop.com/results/?alpha=${l}")
    val indexFuture: Future[List[Anchor]] = extractor[Anchor](urls){ (url, res) =>
      val jsoup = Jsoup.parse(res.body)
      jsoup.select("h3 > a").iterator().toList.map{ elm =>
        Anchor(elm.text(), elm.attr("href"), elm.attr("title"))
      }
    }
    // write the index out as a json file...
    indexFuture.foreach{ index =>
      println("done")
      val jsonList:String = Json.prettyPrint( Json.toJson(index.sortBy(_.text.toLowerCase)) )
      val writer = new PrintWriter("index2.json", "UTF-8")
      println("write......................")
      writer.println(jsonList)
      writer.close()
    }
  }

  def gatherTopicSites = {

    new FileOutputStream("topics.json").close() // truncate file

    val json = Files.readAllLines(FileSystems.getDefault().getPath(".", "index.json")).mkString("")
    val indexAnchors = Json.parse(json).as[List[Anchor]]

    val groups = indexAnchors.grouped(10).toList

    groups.foreach { g =>
      val topicFuture: Future[List[TopicAnchor]] = Future.sequence(g.map { a =>
        val topic = a.text
        val url = a.href
        println(s"Topic ${topic}")
        extractor[Anchor](List(url)) { (url, res) =>
          val jsoup = Jsoup.parse(res.body)
          jsoup.select("li.site h2 > a").iterator().toList.map{ elm =>
            Anchor(elm.text(), elm.attr("href"), "")
          }
        }.map(l => TopicAnchor(topic, url, l))
      })

      val topic = Await.result(topicFuture, 5 minutes)    // throttle so we don't run into problems

      println("done")
      val jsonList: String = Json.prettyPrint(Json.toJson(topic.sortBy(_.topic.toLowerCase)))
      val writer = new PrintWriter(new FileOutputStream(new File("topics.json"), true))
      println("write...........................................................................")
      writer.println(jsonList)
      writer.close()

    }
  }

  def etl = {

    val skip = 0
    //new FileOutputStream("/media/suroot/files/data/alltop/etl.json").close() // truncate file
    //new FileOutputStream("/media/suroot/files/data/alltop/rss.json").close() // truncate file

    val json = Files.readAllLines(FileSystems.getDefault().getPath(".", "topics.json")).mkString("")
    val indexAnchors = Json.parse(json).as[List[TopicAnchor]]

    println(s"Parsed ${indexAnchors.size} TopicAnchors")
    val topics = indexAnchors.drop(skip)

    val total = topics.size

    topics.zipWithIndex.foreach { case (g, ind) =>
      println(s"processing batch ${(ind+skip)} of ${total}")
      val urls = g.links.map(_.href)
      val etlFuture = extractor[EntityMeta](urls) { (url, res) =>
        List(doHtmlEtl(new URI(url), "text/html", res.body))
      }

      val meta:List[EntityMeta] = Await.result(etlFuture, 5 minutes)    // throttle so we don't run into problems
      val topicMeta = TopicEtl(g.topic, g.url, meta)
      val rss = TopicRss(g.topic, g.url, meta.filter(_.rss.size > 0).map{ s =>
        Rss(s.uri, s.rss)
      })
      val jsonList: String = Json.prettyPrint(Json.toJson(topicMeta))
      val writer = new PrintWriter(new FileOutputStream(new File("/media/suroot/files/data/alltop/etl.json"), true))
      println("write META...........................................................................")
      writer.println(jsonList)
      writer.close()

      val rssList: String = Json.prettyPrint(Json.toJson(rss))
      val writer2 = new PrintWriter(new FileOutputStream(new File("/media/suroot/files/data/alltop/rss.json"), true))
      println("write RSS ...........................................................................")
      writer2.println(rssList)
      writer2.close()
      println(s"done.. ${(ind+skip)}")

    }

  }


  def doHtmlEtl(actualUrl:URI, contentType:String, body:String):EntityMeta = {
    import scala.collection.JavaConversions._
    // TODO: make a custom build of Goose.. and do image resize and full meta analysis there.. remove jsoup
    val jsoup = Jsoup.parse(body)
    val cnf = new Configuration()
    cnf.setEnableImageFetching(false)
    val goose = new Goose(cnf)
    val article = goose.extractContent(actualUrl.toString, body)
    println(article.title)
    val title = article.title
    val meta = jsoup.select("meta").iterator().map{ elm =>
      elm.attr("name") -> elm.attr("content")
    }.toMap
    val link = jsoup.select("meta").iterator()
    val possibleRss = jsoup.select("a").iterator().toList.filter(l => l.hasAttr("href") && (l.attr("href").contains("feed") || l.attr("href").contains("rss") )  ).map(_.attr("href")).toSet
    val keywords:Set[String] = meta.get("keywords").map(x => x.split(",").toSet).getOrElse(Set.empty[String])
    val rssRelative:Set[String] = link.filter(l => l.hasAttr("type") && (l.attr("type").contains("rss") || l.attr("type").contains("atom"))  && l.hasAttr("href") ).map(_.attr("href")).toSet ++ possibleRss
    val rssAbs = rssRelative.map{ l =>
      val abs = if(l.contains("://")) l
        else UriBuilder.fromUri(actualUrl).replacePath(l).build().toString
      abs
    }
    val description = article.metaDescription
    val authors = meta.get("og:author").getOrElse(meta.get("author").getOrElse("")).split(",").filter(_ != "").toSet
    val imgs = jsoup.select("img").iterator.map(_.attr("src")).toSet
    val cover = meta.get("og:image") match{
      case Some(img) => img
      case None => meta.get("twitter:image:src") match{
        case Some(img) => img
        case None =>
          imgs.headOption.getOrElse("")
      }
    }
    val version = "0.0.1"
    val content = article.cleanedArticleText
    val etl = EntityMeta(
      version = version,
      uri = actualUrl.toString,
      rss = rssAbs,
      timestamp = DateTime.now().toString,
      icon = s"http://www.google.com/s2/favicons?domain=${actualUrl.toString}",
      thumb = cover,
      publishDate = Try(article.publishDate.toDateTimeISO.toString).toOption,
      domain = article.domain,
      contentType = contentType,
      title = title,
      description = description,
      authors = authors,
      keywords = keywords,
      coverUrl = cover,
      imgs = imgs,
      meta = meta,
      content = Some(content),
      raw = Some(body)
    )

    val entityName = URLEncoder.encode(actualUrl.toString,"UTF-8")
    val filename = s"${entityName}/${EntityMeta.getClass.getCanonicalName}-${version}"
   /* val json = mapper.writeValueAsString(etl)
    // FIXME: temp tester
    println(filename)
    try {
      val file = new File(s"/tmp/etl/${filename}")
      file.getParentFile().mkdirs()
      val writer = new PrintWriter(s"/tmp/etl/${filename}", "UTF-8")
      writer.print(json)
      writer.close()
    }catch{
      case t:Throwable => println(t)
    }*/
    //val writer = new PrintWriter(s"/tmp/html/${filename}", "UTF-8")
    //writer.println(mapper.writeValueAsString(etl))
    //writer.close()

    etl.copy(raw = None) // don't send raw data back
  }


  def cleanRss = {

    val skip = 1001
    //new FileOutputStream("/media/suroot/files/data/alltop/cleanrss.json").close() // truncate file
    //new FileOutputStream("/media/suroot/files/data/alltop/rss.json").close() // truncate file

    val json = Files.readAllLines(FileSystems.getDefault().getPath("/media/suroot/files/data/alltop/", "rss.json")).mkString("")
    val topicRss = Json.parse(json).as[List[TopicRss]]

    println(s"Parsed ${topicRss.size} RSS")
    val topics = topicRss.drop(skip)

    val total = topics.size

    topics.zipWithIndex.foreach { case (g, ind) =>
      println(s"processing batch ${(ind+skip)} of ${total+skip}")
      val topic = g.topic
      val rssSites = g.sites.map{ s =>
        val site = s.site
        val urls = s.rss.filter(u => ! u.contains("comment")) // filter out comment rss
        val cleanFuture: Future[List[String]] = extractor[String](urls.toList) { (url, res) =>
          // Get the content type
          val contentType = res.header("Content-Type").getOrElse("text/html")
          contentType match{
            case ct if ct.contains("xml") =>
              List(url)
            case _ =>
              Nil
          }
        }
        val clean:List[String] = Await.result(cleanFuture, 2 minutes)    // throttle so we don't run into problems
        Rss(site, clean.toSet)
      }.filter(_.rss.size > 0)
      if( rssSites.size > 0 ) {
        val tRss = TopicRss(topic = topic, url = g.url, sites = rssSites)

        val rssList: String = Json.prettyPrint(Json.toJson(tRss))
        val writer2 = new PrintWriter(new FileOutputStream(new File("/media/suroot/files/data/alltop/cleanrss.json"), true))
        println("write RSS ...........................................................................")
        writer2.println(rssList)
        writer2.close()
        println(s"done.. ${(ind + skip)}")
      }
    }

  }

  override def main(args: Array[String]) {

    //extractIndex

    //gatherTopicSites

    //etl

    cleanRss

    // TODO: hit site get meta data try to extract rss feed..
  }
}
