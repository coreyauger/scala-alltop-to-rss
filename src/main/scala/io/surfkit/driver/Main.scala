package io.surfkit.driver

import java.io.{File, PrintWriter}
import java.nio.file.{FileSystems, Path, Files}

import io.surfkit.data.Data
import io.surfkit.data.Data.Anchor
import org.jsoup.Jsoup
import play.api.libs.ws.ning._
import play.api.libs.ws._
import play.api.libs.json._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * Created by Corey Auger
 */

object Main extends App{



  override def main(args: Array[String]) {
    // alpha range
    val alpha = 'a' to 'z'

    val ws = {
      val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
      new play.api.libs.ws.ning.NingWSClient(builder.build())
    }

   /* val indexFuture: Future[List[Anchor]] = Future.sequence( alpha.toList.map{ letter =>
      val url = s"http://alltop.com/results/?alpha=${letter}"
      println(url)
      ws.url(url).withFollowRedirects(true).withRequestTimeout(20000).get.map {
        req =>
          println(s"Got: ${letter}")

          val jsoup = Jsoup.parse(req.body)
          jsoup.select("h3 > a").iterator().map{ elm =>
            Anchor(elm.text(), elm.attr("href"), elm.attr("title"))
          }.toList
      }
    }).map(_.flatten)

    // write the index out as a json file...
    indexFuture.foreach{ index =>
      println("done")
      val jsonList:String = Json.prettyPrint( Json.toJson(index.sortBy(_.text.toLowerCase)) )
      val writer = new PrintWriter("index.json", "UTF-8")
      println("write......................")
      writer.println(jsonList)
      writer.close()
    }*/

    val json = Files.readAllLines(FileSystems.getDefault().getPath(".", "index.json")).mkString("")
    val indexAnchors = Json.parse(json).as[List[Anchor]]
    println(indexAnchors)



    //val flatListFuture:Future[List[Anchor]] = indexFuture.

    //index.
    //val jsoup = Jsoup.parse(body)
  }
}
