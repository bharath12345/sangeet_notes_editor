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
    leftPanelCollapsed: Boolean = false,
    bottomPanelCollapsed: Boolean = false,
    rightPanelCollapsed: Boolean = false,
    theme: String = "light",
    // Phase 13 task 5: frequent users can suppress the read-only Yaman sample that loads
    // on startup when there's no prior session. Default true so the welcome-experience
    // stays unchanged for new installs. Toggled false via the in-tab "Don't show on
    // startup" button; can be re-enabled from the About dialog.
    showSampleOnStartup: Boolean = true
)
