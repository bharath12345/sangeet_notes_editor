package com.varpas.sangeet.server.routes

import cats.effect.IO
import io.circe.Json
import sttp.model.StatusCode
import com.varpas.sangeet.core.api.ApiError
import com.varpas.sangeet.server.{ApiEnvelope, ErrorMapping}

object RouteHelper:

  def handleResult[A](result: Either[ApiError, A])(encode: A => Json): IO[Either[(StatusCode, Json), Json]] =
    result match
      case Right(a)   => IO.pure(Right(ApiEnvelope.successRaw(encode(a))))
      case Left(err)  => IO.pure(Left(ErrorMapping.toResponse(err)))
