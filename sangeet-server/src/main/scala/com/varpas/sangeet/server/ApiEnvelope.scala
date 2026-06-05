package com.varpas.sangeet.server

import io.circe.Json

object ApiEnvelope:

  def success[A](data: A)(using enc: io.circe.Encoder[A]): Json =
    Json.obj(
      "success" -> Json.True,
      "data"    -> enc(data)
    )

  def successRaw(data: Json): Json =
    Json.obj(
      "success" -> Json.True,
      "data"    -> data
    )

  def failure(code: String, message: String): Json =
    Json.obj(
      "success" -> Json.False,
      "error" -> Json.obj(
        "code"    -> Json.fromString(code),
        "message" -> Json.fromString(message)
      )
    )
