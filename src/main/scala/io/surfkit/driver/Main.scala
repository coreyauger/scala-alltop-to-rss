package io.surfkit.driver

import java.io.{FileOutputStream, File, PrintWriter}
import java.nio.file.{FileSystems, Path, Files}

import io.surfkit.data.Data
import io.surfkit.data.Data.{TopicAnchor, Anchor}
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

  def extractor[T](urls: List[String], select: String)(f: (Element) => T):Future[List[T]] = {
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
  }


  def extractIndex() = {
    // alpha range
    val alpha = 'a' to 'z'

    val urls = alpha.toList.map(l => s"http://alltop.com/results/?alpha=${l}")
    val indexFuture: Future[List[Anchor]] = extractor[Anchor](urls, "h3 > a"){ elm =>
      Anchor(elm.text(), elm.attr("href"), elm.attr("title"))
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
        extractor[Anchor](List(url), "li.site h2 > a") { elm =>
          Anchor(elm.text(), elm.attr("href"), "")
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

  override def main(args: Array[String]) {

    //extractIndex

    //gatherTopicSites

    // TODO: hit site get meta data try to extract rss feed..
  }
}
