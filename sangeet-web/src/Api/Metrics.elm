module Api.Metrics exposing (incrementCounter)

{-| Fire-and-forget HTTP client for `POST /api/v1/metrics/event` (Plan 18 PR-3b).

Each call emits a single Cmd that increments one Micrometer counter on the
server. The response body (204 No Content on success; 400 with diagnostic
JSON on whitelist rejection) is intentionally discarded — we never want to
slow the editor down waiting for an analytics POST, and we definitely don't
want to surface analytics failures to the user. The HTTP callback resolves
to `State.Msg.NoOp` so the resulting Msg is a true no-op.

Cardinality is enforced server-side (`AppMetrics.AllowedCounters`); the
client is free to call with whatever strings, but only the whitelist will
actually result in a counter increment. Anything else just returns 400 and
gets dropped.

-}

import Http
import Json.Encode as Encode
import State.Msg exposing (Msg(..))


{-| POST a counter increment. We don't decode the response — see module-level
doc. The server URL is built off `apiBaseUrl` so the same code works in dev
(localhost) and prod (Cloud Run).
-}
incrementCounter : String -> String -> List ( String, String ) -> Cmd Msg
incrementCounter apiBaseUrl counter labels =
    Http.post
        { url = apiBaseUrl ++ "/metrics/event"
        , body =
            Http.jsonBody
                (Encode.object
                    [ ( "counter", Encode.string counter )
                    , ( "labels"
                      , Encode.object
                            (List.map (\( k, v ) -> ( k, Encode.string v )) labels)
                      )
                    ]
                )
        , expect = Http.expectWhatever (\_ -> NoOp)
        }
