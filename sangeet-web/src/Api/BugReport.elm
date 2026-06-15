module Api.BugReport exposing
    ( UncaughtError
    , sendAutomatic
    , uncaughtErrorDecoder
    )

{-| Auto bug-report posting for uncaught JavaScript errors (Plan 18 PR-3c).

The user-initiated bug-report flow lives in `Ports.submitBugReport` because
its payload bundles the rrweb replay buffer assembled in JS. The
auto-capture flow doesn't need the replay buffer (uncaught errors are
intercepted at the browser level, often before the user has interacted at
all), so we keep it pure Elm + Http.

The payload shape is intentionally compatible with the existing
/api/v1/bug-reports endpoint — the only new field is `source: "uncaught"`,
which the server's IssueBuilder reads to distinguish auto-captured reports
from user-initiated ones (different title prefix + label).

Privacy posture: auto-send, no user UI. Stack traces are capped at 8000
characters on the JS side before reaching this module. URL + user-agent
are included so a human triager can reproduce the issue. See
`docs/developer/operations/observability-and-bug-reporting.md` for the
full privacy note.

-}

import Http
import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode



-- TYPES


{-| Mirrors the JS payload pushed through `Ports.uncaughtError`. All
fields except `message`, `url`, and `userAgent` are optional because
`unhandledrejection` reasons might not be Error instances (so no stack)
and don't carry filename/line/col.
-}
type alias UncaughtError =
    { message : String
    , stack : Maybe String
    , filename : Maybe String
    , line : Maybe Int
    , col : Maybe Int
    , url : String
    , userAgent : String
    }



-- DECODER


uncaughtErrorDecoder : Decoder UncaughtError
uncaughtErrorDecoder =
    Decode.map7 UncaughtError
        (Decode.field "message" Decode.string)
        (Decode.field "stack" (Decode.nullable Decode.string))
        (Decode.field "filename" (Decode.nullable Decode.string))
        (Decode.field "line" (Decode.nullable Decode.int))
        (Decode.field "col" (Decode.nullable Decode.int))
        (Decode.field "url" Decode.string)
        (Decode.field "userAgent" Decode.string)



-- COMMAND


{-| Fire-and-forget POST to the bug-report endpoint. The caller passes a
no-op Msg constructor (e.g. `\_ -> NoOp`) because:

  - The user wasn't expecting feedback — there's no UI to update.
  - A failure should not surface to the user (they didn't trigger the
    submit, so a toast would be confusing).
  - Looping is the real risk: if the POST itself fails, we silently
    drop the response. The Elm Http runtime doesn't throw into JS-side
    error handlers, so the listener loop is broken at the boundary.

-}
sendAutomatic :
    String
    -> UncaughtError
    -> (Result Http.Error () -> msg)
    -> Cmd msg
sendAutomatic apiBaseUrl err toMsg =
    Http.post
        { url = stripTrailingSlash apiBaseUrl ++ "/bug-reports"
        , body = Http.jsonBody (encodePayload err)
        , expect = Http.expectWhatever toMsg
        }



-- INTERNAL


{-| Build the request body that mirrors the existing user-submitted
shape — `type`, `description`, `email` (empty for auto-capture), plus the
new `source: "uncaught"` discriminator and a `replay: []` placeholder so
the IssueBuilder's "Replay events captured" line doesn't choke. The stack
trace + raw error metadata go in `description` since IssueBuilder already
renders that field prominently.
-}
encodePayload : UncaughtError -> Encode.Value
encodePayload err =
    Encode.object
        [ ( "type", Encode.string "web" )
        , ( "source", Encode.string "uncaught" )
        , ( "description", Encode.string (buildDescription err) )
        , ( "replay", Encode.list identity [] )
        , ( "metadata"
          , Encode.object
                [ ( "url", Encode.string err.url )
                , ( "userAgent", Encode.string err.userAgent )
                , ( "errorMessage", Encode.string err.message )
                , ( "stack", encodeMaybeString err.stack )
                , ( "filename", encodeMaybeString err.filename )
                , ( "line", encodeMaybeInt err.line )
                , ( "col", encodeMaybeInt err.col )
                ]
          )
        ]


buildDescription : UncaughtError -> String
buildDescription err =
    let
        header : String
        header =
            "Uncaught error: " ++ err.message

        location : String
        location =
            case ( err.filename, err.line ) of
                ( Just fn, Just ln ) ->
                    "\nAt " ++ fn ++ ":" ++ String.fromInt ln

                ( Just fn, Nothing ) ->
                    "\nAt " ++ fn

                _ ->
                    ""

        stackBlock : String
        stackBlock =
            case err.stack of
                Just s ->
                    "\n\nStack:\n" ++ s

                Nothing ->
                    ""
    in
    header ++ location ++ stackBlock


encodeMaybeString : Maybe String -> Encode.Value
encodeMaybeString m =
    case m of
        Just s ->
            Encode.string s

        Nothing ->
            Encode.null


encodeMaybeInt : Maybe Int -> Encode.Value
encodeMaybeInt m =
    case m of
        Just n ->
            Encode.int n

        Nothing ->
            Encode.null


stripTrailingSlash : String -> String
stripTrailingSlash s =
    if String.endsWith "/" s then
        String.dropRight 1 s

    else
        s
