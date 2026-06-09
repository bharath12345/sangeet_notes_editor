package com.varpas.sangeet.core.config

import io.circe._
import io.circe.generic.semiauto._

object ConfigCodecs:
  given Encoder[BookmarkEntry] = deriveEncoder
  given Decoder[BookmarkEntry] = deriveDecoder
  given Encoder[OpenTab]       = deriveEncoder
  given Decoder[OpenTab]       = deriveDecoder
  given Encoder[AppConfig]     = deriveEncoder

  given Decoder[AppConfig] = Decoder.instance { c =>
    for
      bookmarks          <- c.getOrElse[List[BookmarkEntry]]("bookmarks")(Nil)
      openTabs           <- c.getOrElse[List[OpenTab]]("openTabs")(Nil)
      activeTabPath      <- c.getOrElse[Option[String]]("activeTabPath")(None)
      leftPanelWidth     <- c.getOrElse[Double]("leftPanelWidth")(250.0)
      leftPanelCollapsed <- c.getOrElse[Boolean]("leftPanelCollapsed")(false)
    yield AppConfig(bookmarks, openTabs, activeTabPath, leftPanelWidth, leftPanelCollapsed)
  }
