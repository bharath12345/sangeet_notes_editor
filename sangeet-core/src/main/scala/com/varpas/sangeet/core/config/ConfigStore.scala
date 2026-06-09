package com.varpas.sangeet.core.config

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import io.circe.syntax._
import io.circe.parser.{parse => parseJson}

object ConfigStore:
  import ConfigCodecs.given

  private val appName = "sangeet-notes-editor"

  def configDir: Path =
    val os = System.getProperty("os.name", "").toLowerCase
    val base =
      if os.contains("mac") then Path.of(System.getProperty("user.home"), "Library", "Application Support")
      else if os.contains("win") then Path.of(System.getenv("APPDATA"))
      else
        val xdg = System.getenv("XDG_CONFIG_HOME")
        if xdg != null && xdg.nonEmpty then Path.of(xdg)
        else Path.of(System.getProperty("user.home"), ".config")
    base.resolve(appName)

  def configFile: Path = configDir.resolve(s"$appName.json")

  def load(): AppConfig =
    val path = configFile
    if !Files.exists(path) then AppConfig()
    else
      try
        val json = Files.readString(path, StandardCharsets.UTF_8)
        parseJson(json).flatMap(_.as[AppConfig]).getOrElse(AppConfig())
      catch case _: Exception => AppConfig()

  def save(config: AppConfig): Unit =
    val dir = configDir
    if !Files.exists(dir) then Files.createDirectories(dir)
    val json = config.asJson.spaces2
    val tmp  = dir.resolve(s"$appName.json.tmp")
    Files.writeString(tmp, json, StandardCharsets.UTF_8)
    Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING)

  def loadFrom(path: Path): AppConfig =
    if !Files.exists(path) then AppConfig()
    else
      try
        val json = Files.readString(path, StandardCharsets.UTF_8)
        parseJson(json).flatMap(_.as[AppConfig]).getOrElse(AppConfig())
      catch case _: Exception => AppConfig()

  def saveTo(config: AppConfig, path: Path): Unit =
    val dir = path.getParent
    if dir != null && !Files.exists(dir) then Files.createDirectories(dir)
    val json = config.asJson.spaces2
    val tmp  = path.resolveSibling(path.getFileName.toString + ".tmp")
    Files.writeString(tmp, json, StandardCharsets.UTF_8)
    Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
