module Debug.Interpreter exposing (interpret)

{-| Maps an incoming DebugCommand JSON value (produced by the WS bridge) to an
existing State.Msg. Each DebugCommand variant maps to one or more existing Msgs
so the editor logic stays identical to the keyboard path — the bridge is a
back-door for SENDING input, not a parallel editor implementation.

The decoder shape must match circe's encoded shape of
sangeet-core's enum DebugCommand. Circe encodes Scala 3 enums with a discriminator
at top level: { "VariantName": { field1: value1, ... } }.

Commands that require synchronous responses (GetState, DumpComposition, etc.) return
( Msg, Maybe Response ). The response carries the correlated id from the inbound message.

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
    | Help
    | ThreadDump
    | SetDebug Bool
    | ThrowCrash
    | ListTabs
    | SelectTab String
    | NewTab
    | CloseTab String
    | TabInfo
    | Reset { compositionType : String, raag : Maybe String, taal : String }
    | SetTaal String
    | CheckFocus
    | FocusEditor
    | SetOctave String
    | SetSubdivision Int
    | TypeChar String
    | Press String
    | TypeTimed String Int
    | DualSwar String String
    | SwarGroup (List String)
    | Stroke String
    | SimpleOrnament String
    | OrnamentStart String
    | OrnamentNote String
    | FinishOrnament
    | SwitchSection Int
    | GetState
    | GetEvents
    | DumpComposition
    | DumpHistory
    | ExportHtml


commandWithIdDecoder : Decoder ( String, DebugCmd )
commandWithIdDecoder =
    Decode.map2 Tuple.pair
        (Decode.field "id" Decode.string)
        (Decode.field "cmd" cmdDecoder)


cmdDecoder : Decoder DebugCmd
cmdDecoder =
    Decode.oneOf
        [ Decode.field "Ping" (Decode.succeed Ping)
        , Decode.field "Help" (Decode.succeed Help)
        , Decode.field "ThreadDump" (Decode.succeed ThreadDump)
        , Decode.field "SetDebug" setDebugDecoder
        , Decode.field "ThrowCrash" (Decode.succeed ThrowCrash)
        , Decode.field "ListTabs" (Decode.succeed ListTabs)
        , Decode.field "SelectTab" selectTabDecoder
        , Decode.field "NewTab" (Decode.succeed NewTab)
        , Decode.field "CloseTab" closeTabDecoder
        , Decode.field "TabInfo" (Decode.succeed TabInfo)
        , Decode.field "Reset" resetDecoder
        , Decode.field "SetTaal" setTaalDecoder
        , Decode.field "CheckFocus" (Decode.succeed CheckFocus)
        , Decode.field "FocusEditor" (Decode.succeed FocusEditor)
        , Decode.field "SetOctave" setOctaveDecoder
        , Decode.field "SetSubdivision" setSubdivisionDecoder
        , Decode.field "TypeChar" typeCharDecoder
        , Decode.field "Press" pressDecoder
        , Decode.field "TypeTimed" typeTimedDecoder
        , Decode.field "DualSwar" dualSwarDecoder
        , Decode.field "SwarGroup" swarGroupDecoder
        , Decode.field "Stroke" strokeDecoder
        , Decode.field "SimpleOrnament" simpleOrnamentDecoder
        , Decode.field "OrnamentStart" ornamentStartDecoder
        , Decode.field "OrnamentNote" ornamentNoteDecoder
        , Decode.field "FinishOrnament" (Decode.succeed FinishOrnament)
        , Decode.field "SwitchSection" switchSectionDecoder
        , Decode.field "GetState" (Decode.succeed GetState)
        , Decode.field "GetEvents" (Decode.succeed GetEvents)
        , Decode.field "DumpComposition" (Decode.succeed DumpComposition)
        , Decode.field "DumpHistory" (Decode.succeed DumpHistory)
        , Decode.field "ExportHtml" (Decode.succeed ExportHtml)
        ]



-- Decoders for each variant


setDebugDecoder : Decoder DebugCmd
setDebugDecoder =
    Decode.map SetDebug (Decode.field "enabled" Decode.bool)


selectTabDecoder : Decoder DebugCmd
selectTabDecoder =
    Decode.map SelectTab (Decode.field "id" Decode.string)


closeTabDecoder : Decoder DebugCmd
closeTabDecoder =
    Decode.map CloseTab (Decode.field "id" Decode.string)


resetDecoder : Decoder DebugCmd
resetDecoder =
    Decode.map3 (\t r ta -> Reset { compositionType = t, raag = r, taal = ta })
        (Decode.field "compositionType" Decode.string)
        (Decode.maybe (Decode.field "raag" Decode.string))
        (Decode.field "taal" Decode.string)


setTaalDecoder : Decoder DebugCmd
setTaalDecoder =
    Decode.map SetTaal (Decode.field "taal" Decode.string)


setOctaveDecoder : Decoder DebugCmd
setOctaveDecoder =
    Decode.map SetOctave (Decode.field "octave" Decode.string)


setSubdivisionDecoder : Decoder DebugCmd
setSubdivisionDecoder =
    Decode.map SetSubdivision (Decode.field "n" Decode.int)


typeCharDecoder : Decoder DebugCmd
typeCharDecoder =
    Decode.map TypeChar (Decode.field "ch" Decode.string)


pressDecoder : Decoder DebugCmd
pressDecoder =
    Decode.map Press (Decode.field "key" Decode.string)


typeTimedDecoder : Decoder DebugCmd
typeTimedDecoder =
    Decode.map2 TypeTimed
        (Decode.field "ch" Decode.string)
        (Decode.field "delayMs" Decode.int)


dualSwarDecoder : Decoder DebugCmd
dualSwarDecoder =
    Decode.map2 DualSwar
        (Decode.field "first" Decode.string)
        (Decode.field "second" Decode.string)


swarGroupDecoder : Decoder DebugCmd
swarGroupDecoder =
    Decode.map SwarGroup (Decode.field "notes" (Decode.list Decode.string))


strokeDecoder : Decoder DebugCmd
strokeDecoder =
    Decode.map Stroke (Decode.field "stroke" Decode.string)


simpleOrnamentDecoder : Decoder DebugCmd
simpleOrnamentDecoder =
    Decode.map SimpleOrnament (Decode.field "name" Decode.string)


ornamentStartDecoder : Decoder DebugCmd
ornamentStartDecoder =
    Decode.map OrnamentStart (Decode.field "kind" Decode.string)


ornamentNoteDecoder : Decoder DebugCmd
ornamentNoteDecoder =
    Decode.map OrnamentNote (Decode.field "note" Decode.string)


switchSectionDecoder : Decoder DebugCmd
switchSectionDecoder =
    Decode.map SwitchSection (Decode.field "idx" Decode.int)



-- Command application


applyCmd : String -> DebugCmd -> Model -> ( Msg, Maybe Response )
applyCmd id cmd model =
    case cmd of
        Ping ->
            ( NoOp
            , Just { id = id, result = Encode.string "PONG", error = Nothing }
            )

        Help ->
            ( NoOp
            , Just { id = id, result = Encode.string helpText, error = Nothing }
            )

        ThreadDump ->
            -- Browser doesn't expose thread dumps. Return placeholder.
            ( NoOp
            , Just { id = id, result = Encode.string "thread-dump: browser-only (no threads)", error = Nothing }
            )

        SetDebug _ ->
            -- No equivalent debug toggle on web. Accept for parity, no-op.
            ( NoOp, Nothing )

        ThrowCrash ->
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "crash injection not supported on web" }
            )

        ListTabs ->
            let
                tabs =
                    encodeTabsList model
            in
            ( NoOp, Just { id = id, result = tabs, error = Nothing } )

        SelectTab tabId ->
            ( SwitchTab tabId, Nothing )

        NewTab ->
            ( State.Msg.NewTab, Nothing )

        CloseTab tabId ->
            ( State.Msg.CloseTab tabId, Nothing )

        TabInfo ->
            let
                info =
                    encodeTabInfo model
            in
            ( NoOp, Just { id = id, result = info, error = Nothing } )

        Reset _ ->
            -- TODO(plan-14 Phase 9): wire Reset to NewDialog API flow or direct composition-reset
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "Reset not fully implemented" }
            )

        SetTaal taal ->
            -- TODO(plan-14 Phase 9): wire SetTaal to PropsDialog API flow or direct taal-change
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "SetTaal not fully implemented" }
            )

        CheckFocus ->
            -- Web has no JavaFX-style focus concept. Always report "true".
            ( NoOp
            , Just { id = id, result = Encode.bool True, error = Nothing }
            )

        FocusEditor ->
            -- No-op on web
            ( NoOp, Nothing )

        SetOctave oct ->
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

        TypeChar ch ->
            ( KeyPressed ch False False False, Nothing )

        Press key ->
            -- Map named keys (e.g. "BACKSPACE" → "Backspace")
            let
                mappedKey =
                    case key of
                        "BACKSPACE" ->
                            "Backspace"

                        "DELETE" ->
                            "Delete"

                        "ENTER" ->
                            "Enter"

                        "TAB" ->
                            "Tab"

                        "ESCAPE" ->
                            "Escape"

                        _ ->
                            key
            in
            ( KeyPressed mappedKey False False False, Nothing )

        TypeTimed ch delayMs ->
            -- TODO(plan-14 Phase 9): wire TypeTimed to delayed KeyPressed via Process.sleep
            -- when a ported test exercises this command.
            ( KeyPressed ch False False False, Nothing )

        DualSwar first second ->
            -- TODO(plan-14 Phase 9): wire DualSwar to sequential KeyPressed or API grouping call
            -- when a ported test exercises this command.
            ( KeyPressed first False False False, Nothing )

        SwarGroup notes ->
            -- TODO(plan-14 Phase 9): wire SwarGroup to API call or KeyPressed sequence
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "SwarGroup not fully implemented" }
            )

        Stroke strokeName ->
            -- TODO(plan-14 Phase 9): wire Stroke to stroke-mode toggle + KeyPressed
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "Stroke not fully implemented" }
            )

        SimpleOrnament name ->
            -- TODO(plan-14 Phase 9): wire SimpleOrnament to Alt+key mapping from KeyHandler
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "SimpleOrnament not fully implemented" }
            )

        OrnamentStart kind ->
            -- TODO(plan-14 Phase 9): wire OrnamentStart to Alt+key mapping from KeyHandler
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "OrnamentStart not fully implemented" }
            )

        OrnamentNote note ->
            ( KeyPressed note False False False, Nothing )

        FinishOrnament ->
            ( KeyPressed "Enter" False False False, Nothing )

        SwitchSection idx ->
            ( SelectSection idx, Nothing )

        GetState ->
            let
                snapshot =
                    encodeStateSnapshot model
            in
            ( NoOp, Just { id = id, result = snapshot, error = Nothing } )

        GetEvents ->
            -- TODO(plan-14 Phase 9): wire GetEvents to encode current cursor's beat events
            -- when a ported test exercises this command.
            let
                events =
                    encodeEvents model
            in
            ( NoOp, Just { id = id, result = events, error = Nothing } )

        DumpComposition ->
            -- TODO(plan-14 Phase 9): wire DumpComposition to async serializeComposition API call
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "DumpComposition async not wired" }
            )

        DumpHistory ->
            -- TODO(plan-14 Phase 9): wire DumpHistory to encode full undo/redo stack
            -- when a ported test exercises this command.
            let
                history =
                    encodeHistory model
            in
            ( NoOp, Just { id = id, result = history, error = Nothing } )

        ExportHtml ->
            -- TODO(plan-14 Phase 9): wire ExportHtml to async exportHtml API call
            -- when a ported test exercises this command.
            ( NoOp
            , Just { id = id, result = Encode.null, error = Just "ExportHtml async not wired" }
            )



-- Response encoders


helpText : String
helpText =
    "Sangeet Web Debug Bridge - available commands: Ping, TypeChar, SetOctave, SetSubdivision, SwitchSection, GetState, etc."


encodeStateSnapshot : Model -> Encode.Value
encodeStateSnapshot model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        eventCount =
            comp.sections
                |> List.concatMap .events
                |> List.length

        currentSection =
            comp.sections
                |> List.drop model.currentSectionIndex
                |> List.head
                |> Maybe.map .name
                |> Maybe.withDefault ""
    in
    Encode.object
        [ ( "ok", Encode.bool True )
        , ( "eventCount", Encode.int eventCount )
        , ( "cursorBeat", Encode.int cur.beat )
        , ( "cursorCycle", Encode.int cur.cycle )
        , ( "sectionName", Encode.string currentSection )
        , ( "taalName", Encode.string comp.metadata.taal.name )
        , ( "raagName", Encode.string comp.metadata.raag.name )
        , ( "sectionCount", Encode.int (List.length comp.sections) )
        ]


encodeTabsList : Model -> Encode.Value
encodeTabsList model =
    Encode.list
        (\t ->
            Encode.object
                [ ( "id", Encode.string t.id )
                , ( "filename", Encode.string t.filename )
                ]
        )
        model.tabs


encodeTabInfo : Model -> Encode.Value
encodeTabInfo model =
    let
        comp =
            Model.composition model
    in
    Encode.object
        [ ( "id", Encode.string "main" )
        , ( "filename", Encode.string "composition.swar" )
        , ( "taal", Encode.string comp.metadata.taal.name )
        ]


encodeEvents : Model -> Encode.Value
encodeEvents model =
    -- TODO: encode the events at the current cursor position
    Encode.list identity []


encodeHistory : Model -> Encode.Value
encodeHistory model =
    -- TODO: encode undo/redo stack depth
    Encode.object
        [ ( "undoDepth", Encode.int 0 )
        , ( "redoDepth", Encode.int 0 )
        ]
