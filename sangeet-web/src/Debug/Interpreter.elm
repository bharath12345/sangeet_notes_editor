module Debug.Interpreter exposing (interpret)

{-| Maps an incoming DebugCommand JSON value (produced by the WS bridge) to an
existing State.Msg. Each DebugCommand variant maps to one or more existing Msgs
so the editor logic stays identical to the keyboard path — the bridge is a
back-door for SENDING input, not a parallel editor implementation.

The decoder shape must match circe's encoded shape of
sangeet-core's enum DebugCommand. If circe is configured for default sealed-trait
encoding, that's: { "VariantName": { field1: value1, ... } } at the top level.

If a command requires a synchronous response back over WS (GetState, DumpComposition),
the interpreter returns ( Maybe Msg, Maybe DebugResponse ). The response carries the
correlated id from the inbound message.

See docs/developer/debug-bridge.md for the wire format.

-}

import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))


type alias Response =
    { id : String, result : Encode.Value, error : Maybe String }


{-| Apply a DebugCommand JSON value to the model. Returns:

  - the Msg to dispatch (or NoOp if the command is purely a state read)
  - an optional Response to send back over WS (for state-read commands)

-}
interpret : Decode.Value -> Model -> ( Msg, Maybe Response )
interpret raw model =
    case Decode.decodeValue commandWithIdDecoder raw of
        Err _ ->
            ( NoOp, Just { id = "", result = Encode.null, error = Just "decode failed" } )

        Ok ( id, cmd ) ->
            applyCmd id cmd model


type DebugCmd
    = Ping
    | Reset { compositionType : String, raag : Maybe String, taal : String }
    | TypeChar String
    | SetOctave String
    | SetSubdivision Int
    | GetState
    | DumpComposition
      -- TODO Phase 4.5: add remaining variants
    | UnknownCmd String


commandWithIdDecoder : Decoder ( String, DebugCmd )
commandWithIdDecoder =
    Decode.map2 Tuple.pair
        (Decode.field "id" Decode.string)
        (Decode.field "cmd" cmdDecoder)


cmdDecoder : Decoder DebugCmd
cmdDecoder =
    Decode.oneOf
        [ Decode.field "Ping" (Decode.succeed Ping)
        , Decode.field "Reset" resetDecoder
        , Decode.field "TypeChar" typeCharDecoder
        , Decode.field "SetOctave" setOctaveDecoder
        , Decode.field "SetSubdivision" setSubdivisionDecoder
        , Decode.field "GetState" (Decode.succeed GetState)
        , Decode.field "DumpComposition" (Decode.succeed DumpComposition)
        , Decode.map UnknownCmd (Decode.succeed "unknown")
        ]


resetDecoder : Decoder DebugCmd
resetDecoder =
    Decode.map3 (\t r ta -> Reset { compositionType = t, raag = r, taal = ta })
        (Decode.field "compositionType" Decode.string)
        (Decode.maybe (Decode.field "raag" Decode.string))
        (Decode.field "taal" Decode.string)


typeCharDecoder : Decoder DebugCmd
typeCharDecoder =
    Decode.map TypeChar (Decode.field "ch" Decode.string)


setOctaveDecoder : Decoder DebugCmd
setOctaveDecoder =
    Decode.map SetOctave (Decode.field "octave" Decode.string)


setSubdivisionDecoder : Decoder DebugCmd
setSubdivisionDecoder =
    Decode.map SetSubdivision (Decode.field "n" Decode.int)


applyCmd : String -> DebugCmd -> Model -> ( Msg, Maybe Response )
applyCmd id cmd model =
    case cmd of
        Ping ->
            ( NoOp
            , Just { id = id, result = Encode.string "PONG", error = Nothing }
            )

        Reset r ->
            -- Reset is a composite of: dismiss any open dialog, then create the
            -- composition via the New Composition flow. For now, dispatch the
            -- equivalent NewDialog* + Submit Msgs in sequence. Phase 4.5 will
            -- wire this end-to-end after we read State/Update for the exact
            -- composition-creation Msg sequence.
            ( NoOp, Nothing )

        TypeChar ch ->
            -- Synthesize a KeyPressed Msg as if the user typed the character.
            ( KeyPressed ch False False False, Nothing )

        SetOctave oct ->
            -- The existing keyboard binding for taar octave is "]"; mandra is "[";
            -- madhya is "\\". Map the string to the equivalent KeyPressed.
            let
                key =
                    case oct of
                        "mandra" ->
                            "["

                        "taar" ->
                            "]"

                        _ ->
                            "\\"
            in
            ( KeyPressed key False False False, Nothing )

        SetSubdivision n ->
            ( KeyPressed (String.fromInt n) False False False, Nothing )

        GetState ->
            let
                snapshot =
                    encodeStateSnapshot model
            in
            ( NoOp, Just { id = id, result = snapshot, error = Nothing } )

        DumpComposition ->
            let
                comp =
                    encodeComposition model
            in
            ( NoOp, Just { id = id, result = comp, error = Nothing } )

        UnknownCmd _ ->
            ( NoOp, Just { id = id, result = Encode.null, error = Just "unknown command" } )


{-| Encode a small subset of Model for state-check assertions. Keep the shape
stable across versions: tests assert specific fields, so adding fields is fine
but renaming or removing them is a breaking change.
-}
encodeStateSnapshot : Model -> Encode.Value
encodeStateSnapshot _ =
    Encode.object
        [ ( "ok", Encode.bool True )

        -- TODO Phase 4.5: populate with eventCount, cursorBeat, cursorCycle,
        -- sectionName, etc. based on what test checkpoints need.
        ]


encodeComposition : Model -> Encode.Value
encodeComposition _ =
    Encode.null



-- TODO Phase 4.5: encode the full composition or call the server's serialize endpoint
