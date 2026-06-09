package com.varpas.sangeet.core.config

case class BookmarkEntry(
    path: String,
    isDirectory: Boolean,
    label: String
)

case class OpenTab(
    filePath: String,
    sectionIndex: Int
)

case class AppConfig(
    bookmarks: List[BookmarkEntry] = Nil,
    openTabs: List[OpenTab] = Nil,
    activeTabPath: Option[String] = None,
    leftPanelWidth: Double = 250.0,
    leftPanelCollapsed: Boolean = false
)
