import sbt.Keys._

name := "scala-alltop-to-rss"

version := "1.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= deps

scalaVersion := "2.11.7"

lazy val deps = {
  val akkaV = "2.4.1"
  val sparkV = "1.6.0"
  val akkaStreamV = "1.0"
  Seq(
    "org.jsoup"              %  "jsoup"                              % "1.8.3",
    "com.gravity"            %% "goose"                              % "2.1.25-SNAPSHOT",
    "com.lihaoyi"            %% "upickle"                            % "0.3.6",
    "com.typesafe.akka"      %% "akka-stream-experimental"           % akkaStreamV,
    "com.typesafe.akka"      %% "akka-http-core-experimental"        % akkaStreamV,
    "org.apache.spark"       %% "spark-core"                         % sparkV,
    "org.apache.spark"       %% "spark-sql"                          % sparkV
  )
}

