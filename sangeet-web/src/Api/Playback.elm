module Api.Playback exposing (schedulePlayback)

import Api.Client exposing (ApiResult)
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (Composition, encodeComposition)
import Model.Layout exposing (TimedNote, timedNoteDecoder)


{-| Schedule playback events for a composition at a given BPM.
Returns a list of timed notes for the frontend to play via Web Audio API.
-}
schedulePlayback :
    String
    -> Composition
    -> Float
    -> (Result Http.Error (ApiResult (List TimedNote)) -> msg)
    -> Cmd msg
schedulePlayback baseUrl composition bpm onResult =
    Api.Client.postJson
        { url = baseUrl ++ "/playback/schedule"
        , body =
            Encode.object
                [ ( "composition", encodeComposition composition )
                , ( "bpm", Encode.float bpm )
                ]
        , decoder = Decode.list timedNoteDecoder
        , onResult = onResult
        }
