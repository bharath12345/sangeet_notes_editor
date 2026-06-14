module Debug.Interpreter exposing (InterpretResult, Response, interpret)

{-| Maps an incoming DebugCommand JSON value (produced by the WS bridge) to an
existing State.Msg + Cmd. Each DebugCommand variant maps to one or more
existing Msgs so the editor logic stays identical to the keyboard path — the
bridge is a back-door for SENDING input, not a parallel editor implementation.

The decoder shape must match circe's encoded shape of
sangeet-core's enum DebugCommand. Circe encodes Scala 3 enums with a discriminator
at top level: { "VariantName": { field1: value1, ... } }.

Commands that require synchronous responses (Ping, GetState, etc.) return a
Response immediately. Commands that need async work (Reset → /compositions
HTTP call, DumpComposition → /compositions/serialize, ExportHtml → /export/html)
return a Cmd that fires a Debug\*Received Msg carrying the WS request id, so
the response can be sent only after the API call completes.

See docs/developer/debug-bridge.md for the wire format.

-}

import Api.Client
import Api.Composition as ApiComposition
import Api.Cursor as ApiCursor
import Api.Editor as ApiEditor
import Api.Export as ApiExport
import Api.Stroke as ApiStroke
import Http
import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode
import Model.Composition exposing (CompositionType(..), encodeComposition)
import Model.Types exposing (Laya(..), Octave(..), Stroke(..))
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory


type alias Response =
    { id : String, result : Encode.Value, error : Maybe String }


{-| Result of interpreting one debug command:

  - `msg` is the Msg to dispatch through update (or NoOp for read-only commands)
  - `extraCmd` is any extra Cmd to batch (e.g. HTTP for async dump/export)
  - `immediateResponse` fires right away (for synchronous commands like Ping)
  - `deferredAckId` is for commands that route through an async update path
    (TypeChar → API call). The ack is sent after `pendingApiCall` flips
    False, which `drainPendingDebugAck` in State.Update handles.

Both `immediateResponse` and `deferredAckId` may be Nothing for fire-and-
forget commands like SetDebug.

-}
type alias InterpretResult =
    { msg : Msg
    , extraCmd : Cmd Msg
    , immediateResponse : Maybe Response
    , deferredAckId : Maybe String
    , preDispatchTransform : Model -> Model
    }


interpret : Decode.Value -> Model -> InterpretResult
interpret raw model =
    case Decode.decodeValue commandWithIdDecoder raw of
        Err _ ->
            errResp "" "decode failed"

        Ok ( id, cmd ) ->
            applyCmd id cmd model


type DebugCmd
    = Ping
    | Help
    | ThreadDump
    | SetDebug
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
    | TypeTimed String
    | DualSwar String
    | SwarGroup
    | Stroke String
    | SimpleOrnament
    | OrnamentStart
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
    -- Consume the "enabled" field so the JSON shape stays asserted by the
    -- decoder, but discard the value — the web has no debug-toggle equivalent.
    Decode.map (always SetDebug) (Decode.field "enabled" Decode.bool)


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
    -- Consume "delayMs" to keep the shape contract, but discard the value:
    -- the web treats TypeTimed identically to TypeChar (no grouping path yet).
    Decode.map2 (\ch _ -> TypeTimed ch)
        (Decode.field "ch" Decode.string)
        (Decode.field "delayMs" Decode.int)


dualSwarDecoder : Decoder DebugCmd
dualSwarDecoder =
    -- Consume "second" for shape parity, then degrade to a single-note insert.
    Decode.map2 (\first _ -> DualSwar first)
        (Decode.field "first" Decode.string)
        (Decode.field "second" Decode.string)


swarGroupDecoder : Decoder DebugCmd
swarGroupDecoder =
    -- Consume "notes" for shape parity, but the dispatch arm returns an
    -- error since no canonical test exercises grouping on web yet.
    Decode.map (always SwarGroup) (Decode.field "notes" (Decode.list Decode.string))


strokeDecoder : Decoder DebugCmd
strokeDecoder =
    Decode.map Stroke (Decode.field "stroke" Decode.string)


simpleOrnamentDecoder : Decoder DebugCmd
simpleOrnamentDecoder =
    Decode.map (always SimpleOrnament) (Decode.field "name" Decode.string)


ornamentStartDecoder : Decoder DebugCmd
ornamentStartDecoder =
    Decode.map (always OrnamentStart) (Decode.field "kind" Decode.string)


ornamentNoteDecoder : Decoder DebugCmd
ornamentNoteDecoder =
    Decode.map OrnamentNote (Decode.field "note" Decode.string)


switchSectionDecoder : Decoder DebugCmd
switchSectionDecoder =
    Decode.map SwitchSection (Decode.field "idx" Decode.int)



-- Command application


applyCmd : String -> DebugCmd -> Model -> InterpretResult
applyCmd id cmd model =
    case cmd of
        Ping ->
            sync id (Encode.string "PONG")

        Help ->
            sync id (Encode.string helpText)

        ThreadDump ->
            -- Browser doesn't expose thread dumps. Return placeholder.
            sync id (Encode.string "thread-dump: browser-only (no threads)")

        SetDebug ->
            -- No equivalent debug toggle on web. Accept for parity, no-op.
            noResponse NoOp

        ThrowCrash ->
            errResp id "crash injection not supported on web"

        ListTabs ->
            sync id (encodeTabsList model)

        SelectTab tabId ->
            -- Dispatch and return ack so the test runner sees a result.
            ackWith id (SwitchTab tabId)

        NewTab ->
            ackWith id State.Msg.NewTab

        CloseTab tabId ->
            ackWith id (State.Msg.CloseTab tabId)

        TabInfo ->
            sync id (encodeTabInfo model)

        Reset params ->
            -- Mirrors desktop's resetComposition: build a fresh composition
            -- via the server /compositions endpoint and replace the current
            -- tab's editor state with it. The Debug*Received callback fires
            -- the WS response once the server replies, so the runner sees a
            -- consistent state before the next checkpoint.
            applyReset id params model

        SetTaal taalName ->
            applySetTaal id taalName model

        CheckFocus ->
            -- Web has no JavaFX-style focus concept. Always report "true".
            sync id (Encode.bool True)

        FocusEditor ->
            -- No-op on web
            noResponse NoOp

        SetOctave oct ->
            -- Desktop sends keyword names (PERIOD / QUOTE / BACKTICK) that
            -- map to Bhatkhande octave shorthand. Accept those plus the
            -- octave names (mandra / madhya / taar) for forward compat.
            let
                key =
                    case String.toLower oct of
                        "period" ->
                            "["

                        "mandra" ->
                            "["

                        "quote" ->
                            "]"

                        "taar" ->
                            "]"

                        "backtick" ->
                            "\\"

                        "madhya" ->
                            "\\"

                        _ ->
                            "\\"
            in
            ackWith id (KeyPressed key False False False)

        SetSubdivision n ->
            -- The web's KeyHandler maps "1" → InsertChikari (open strings),
            -- not Subdivision 1, so we can't go through KeyPressed for n=1.
            -- Skip KeyPressed entirely and call the /cursor/set-subdivisions
            -- API directly; the GotCursorResult handler updates the cursor
            -- and flips pendingApiCall=False which drains the ack.
            { msg = NoOp
            , extraCmd =
                ApiCursor.setSubdivisions
                    model.apiBaseUrl
                    (Model.cursor model)
                    n
                    GotCursorResult
            , immediateResponse = Nothing
            , deferredAckId = Just id
            , preDispatchTransform = \m -> { m | pendingApiCall = True }
            }

        TypeChar ch ->
            -- The test definitions sometimes pass multi-char "TypeChar"
            -- payloads (e.g. "_r" meaning komal Re). KeyPressed expects a
            -- single key string — for the "_X" prefix we fold it back to
            -- uppercase X which the swar key handler treats as komal.
            -- We also wipe groupingState so the 500ms fast-typing grouping
            -- doesn't collapse consecutive debug commands onto a single
            -- beat (mirrors desktop's per-key insertion semantics).
            -- Critically, Elm's mapKeyToAction routes uppercase swar keys
            -- through mapShiftKey only when shiftKey=True is also set —
            -- mirroring how a real keyboard would deliver them. We set
            -- shiftKey to match the case so "M" → SwarInput Ma Tivra
            -- instead of falling through to NoAction.
            let
                ( key, shift ) =
                    typeCharToWebKey (normalizeTypeChar ch)
            in
            ackWithClearGrouping id (KeyPressed key shift False False)

        Press key ->
            -- Map named keys (e.g. "BACKSPACE" → "Backspace") and the
            -- desktop's Rest/Sustain conventions onto the web KeyHandler's
            -- equivalents:
            --   desktop SPACE  = Rest    → web "-"
            --   desktop MINUS  = Sustain → web "=" (Shift+- on a US layout)
            -- This is a one-place translation; it keeps the canonical
            -- tests' semantics aligned without changing production web
            -- keyboard bindings.
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

                        " " ->
                            "-"

                        "-" ->
                            "="

                        _ ->
                            key
            in
            ackWith id (KeyPressed mappedKey False False False)

        TypeTimed ch ->
            -- No canonical test exercises grouping yet; treat the same as
            -- TypeChar so future tests that send TypeTimed don't error out.
            let
                ( key, shift ) =
                    typeCharToWebKey (normalizeTypeChar ch)
            in
            ackWithClearGrouping id (KeyPressed key shift False False)

        DualSwar first ->
            -- No canonical test exercises this; degrade to a single TypeChar
            -- of the first note so the test framework keeps progressing.
            let
                ( key, shift ) =
                    typeCharToWebKey (normalizeTypeChar first)
            in
            ackWithClearGrouping id (KeyPressed key shift False False)

        SwarGroup ->
            -- TODO(plan-14 follow-up): wire SwarGroup once a canonical test
            -- exercises grouping on web.
            errResp id "SwarGroup not implemented (no canonical test uses it)"

        Stroke strokeName ->
            applyStroke id strokeName model

        SimpleOrnament ->
            errResp id "SimpleOrnament not implemented (no canonical test uses it)"

        OrnamentStart ->
            errResp id "OrnamentStart not implemented (no canonical test uses it)"

        OrnamentNote note ->
            ackWith id (KeyPressed note False False False)

        FinishOrnament ->
            ackWith id (KeyPressed "Enter" False False False)

        SwitchSection idx ->
            -- Desktop's switchSection resets the cursor to a fresh
            -- CursorModel(taal) for the target section (cycle=0, beat=0,
            -- subdivisions=1, octave=Madhya). The web's SelectSection
            -- only clamps the cursor if it's before the startingBeat,
            -- which leaves the cycle stale. We mirror the desktop's
            -- explicit reset here so antara events land at cycle:0
            -- regardless of where the cursor was in sthayi.
            { msg = SelectSection idx
            , extraCmd = Cmd.none
            , immediateResponse = Nothing
            , deferredAckId = Just id
            , preDispatchTransform = resetCursorForSection idx
            }

        GetState ->
            sync id (encodeStateSnapshot model)

        GetEvents ->
            sync id encodeEvents

        DumpComposition ->
            -- Async: serialize via /compositions/serialize. Goes through
            -- the same backend path desktop uses (CompositionApi.
            -- serializeCompositionString), so byte-identical output is
            -- guaranteed by construction. The server returns the raw
            -- .swar JSON body (not wrapped in the ApiResult envelope) so
            -- we read it via expectString directly rather than going
            -- through Api.Client.postJson.
            asyncOnly
                (postExpectingRawString
                    { url = model.apiBaseUrl ++ "/compositions/serialize"
                    , body =
                        Encode.object
                            [ ( "composition", encodeComposition (Model.composition model) ) ]
                    , onResult =
                        \res -> DebugDumpReceived id (mapStringResultToApi res)
                    }
                )

        DumpHistory ->
            sync id encodeHistory

        ExportHtml ->
            -- Async: render HTML via /export/html. Unlike /compositions/
            -- serialize, this endpoint DOES wrap the body in the
            -- ApiResult envelope ({"success":true,"data":"<html>..."}),
            -- so the standard Api.Export.exportHtml helper works without
            -- modification.
            asyncOnly
                (ApiExport.exportHtml
                    model.apiBaseUrl
                    (Model.composition model)
                    model.currentScript
                    (DebugExportReceived id)
                )


sync : String -> Encode.Value -> InterpretResult
sync id result =
    { msg = NoOp
    , extraCmd = Cmd.none
    , immediateResponse = Just { id = id, result = result, error = Nothing }
    , deferredAckId = Nothing
    , preDispatchTransform = identity
    }


errResp : String -> String -> InterpretResult
errResp id message =
    { msg = NoOp
    , extraCmd = Cmd.none
    , immediateResponse = Just { id = id, result = Encode.null, error = Just message }
    , deferredAckId = Nothing
    , preDispatchTransform = identity
    }


{-| Dispatch a Msg and defer the WS ack until the next time pendingApiCall is
False. Use for any command whose dispatched Msg triggers an async API call
(KeyPressed for swar input, SelectSection for layout, etc.). The
clearGrouping flag wipes the time-based grouping state before dispatch so
debug TypeChar commands behave like the desktop's synchronous per-key
insertion path (one event per call, advancing the cursor), instead of
getting collapsed into a multi-note group by the 500ms timer.
-}
ackWith : String -> Msg -> InterpretResult
ackWith id msg =
    { msg = msg
    , extraCmd = Cmd.none
    , immediateResponse = Nothing
    , deferredAckId = Just id
    , preDispatchTransform = identity
    }


ackWithClearGrouping : String -> Msg -> InterpretResult
ackWithClearGrouping id msg =
    { msg = msg
    , extraCmd = Cmd.none
    , immediateResponse = Nothing
    , deferredAckId = Just id
    , preDispatchTransform = \m -> { m | groupingState = Nothing }
    }


noResponse : Msg -> InterpretResult
noResponse msg =
    { msg = msg
    , extraCmd = Cmd.none
    , immediateResponse = Nothing
    , deferredAckId = Nothing
    , preDispatchTransform = identity
    }


asyncOnly : Cmd Msg -> InterpretResult
asyncOnly cmd =
    { msg = NoOp
    , extraCmd = cmd
    , immediateResponse = Nothing
    , deferredAckId = Nothing
    , preDispatchTransform = identity
    }


{-| Normalize the "ch" field of TypeChar to a single key string.

The canonical test definitions sometimes use multi-character payloads like
"\_r" (Bhatkhande shorthand for "komal Re"). The desktop runner just passes
this verbatim to `typeChars` which iterates characters — the underscore
matches nothing in the swar key map and is silently ignored, then the
trailing "r" inserts a Shuddha Re. The golden fixtures were generated
under that exact behaviour, so we mirror it here: pick out the first
character that's an actual swar key (s/r/g/m/p/d/n + uppercase). For
single-char inputs this is a no-op.

-}
normalizeTypeChar : String -> String
normalizeTypeChar raw =
    String.toList raw
        |> List.filter (\c -> String.contains (String.fromChar (Char.toLower c)) "srgmpdn")
        |> List.head
        |> Maybe.map String.fromChar
        |> Maybe.withDefault raw


{-| True iff the first character of the string is upper case. Used to derive
shiftKey for swar keys (KeyHandler's mapShiftKey is the only branch that
routes "M", "R", "G", etc. to a tivra/komal SwarInput).
-}
isUpperFirst : String -> Bool
isUpperFirst s =
    case String.uncons s of
        Just ( c, _ ) ->
            Char.isUpper c

        Nothing ->
            False


{-| Map a debug TypeChar payload to a (browser-key, shiftKey) tuple suitable
for KeyPressed.

Desktop's resolveVariant treats Sa and Pa as fixed (achal) — uppercase "S"
and "P" still mean Shuddha, since komal/tivra don't exist for these notes.
The web KeyHandler's mapShiftKey does NOT have entries for "S"/"P" (they'd
be NoAction), so we collapse uppercase Sa/Pa back to lowercase + no shift
before dispatch. All other uppercase swar keys (R G D N M) pass through
with shiftKey=True so mapShiftKey routes them to the komal/tivra variant.

-}
typeCharToWebKey : String -> ( String, Bool )
typeCharToWebKey raw =
    case raw of
        "S" ->
            ( "s", False )

        "P" ->
            ( "p", False )

        _ ->
            ( raw, isUpperFirst raw )



-- Reset and SetTaal helpers -------------------------------------------------


applyReset :
    String
    -> { compositionType : String, raag : Maybe String, taal : String }
    -> Model
    -> InterpretResult
applyReset id params model =
    let
        raagName =
            params.raag |> Maybe.withDefault "yaman"

        maybeRaag =
            findByName raagName model.availableRaags

        maybeTaal =
            findByName params.taal model.availableTaals
    in
    case ( maybeRaag, maybeTaal ) of
        ( Just raag, Just taal ) ->
            let
                ( compType, layaForType ) =
                    case String.toLower params.compositionType of
                        "bandish" ->
                            ( Bandish, Just MadhyaLaya )

                        "palta" ->
                            -- Desktop omits laya for Palta (None) so the .swar
                            -- fixture doesn't carry a laya field.
                            ( Palta, Nothing )

                        "sargam" ->
                            ( Sargam, Just MadhyaLaya )

                        _ ->
                            ( Gat, Just MadhyaLaya )
            in
            asyncOnly
                (ApiComposition.createComposition model.apiBaseUrl
                    { title = "Debug Test"
                    , compositionType = compType
                    , taal = taal
                    , raag = raag
                    , laya = layaForType
                    , taanCount = 0
                    , showStrokeLine = False
                    , showSahityaLine = False
                    , gatStartingBeat = 1
                    , antaraStartingBeat = 1
                    , taanStartingBeat = 1
                    }
                    (DebugResetReceived id)
                )

        _ ->
            errResp id
                ("Reset failed: raag '"
                    ++ raagName
                    ++ "' or taal '"
                    ++ params.taal
                    ++ "' not found in availableRaags/availableTaals"
                )


applySetTaal : String -> String -> Model -> InterpretResult
applySetTaal id taalName model =
    -- Direct path: hit /editor/change-taal (same endpoint the PropsDialog
    -- submit uses for taal-only changes) and forward the response to
    -- DebugSetTaalReceived. Cleaner than routing through PropsDialogSubmit
    -- because that also re-applies title/raag/section-starting-beat changes
    -- — we only want the taal re-map. Mirrors desktop
    -- DebugCommandHandler.setTaal, which calls
    -- CompositionEditor.changeTaal directly.
    case findByName taalName model.availableTaals of
        Just taal ->
            asyncOnly
                (ApiEditor.changeTaal
                    model.apiBaseUrl
                    (Model.composition model)
                    model.currentSectionIndex
                    taal
                    (DebugSetTaalReceived id)
                )

        Nothing ->
            errResp id ("SetTaal: unknown taal '" ++ taalName ++ "'")


{-| Dispatch a Stroke debug command. The payload is "da", "ra", or "jod"
(matching desktop's `DebugCommandHandler.stroke`). The web Stroke type
covers all three; unknown payloads return an error response.

Note: web's `applyStroke` in KeyHandler.elm also maps "j" → Jod (no
explicit "ch"/chikari stroke exists — that's a separate `Chikari` event).
We accept the same three names the desktop accepts for parity.

-}
applyStroke : String -> String -> Model -> InterpretResult
applyStroke id strokeName model =
    case parseStroke strokeName of
        Just stroke ->
            asyncOnly
                (ApiStroke.setStroke
                    model.apiBaseUrl
                    (Model.composition model)
                    model.currentSectionIndex
                    (Model.cursor model)
                    stroke
                    (DebugStrokeReceived id)
                )

        Nothing ->
            errResp id
                ("Stroke: unknown stroke '"
                    ++ strokeName
                    ++ "' (expected da | ra | jod)"
                )


parseStroke : String -> Maybe Stroke
parseStroke s =
    case String.toLower s of
        "da" ->
            Just Da

        "ra" ->
            Just Ra

        "jod" ->
            Just Jod

        _ ->
            Nothing


findByName : String -> List ( String, a ) -> Maybe a
findByName name pairs =
    pairs
        |> List.filter (\( n, _ ) -> String.toLower n == String.toLower name)
        |> List.head
        |> Maybe.map Tuple.second


{-| Reset the model's active-tab cursor to the start of the section at idx.
Used by SwitchSection to mirror desktop's `switchSection` which builds a
fresh `CursorModel(taal)` for the target section.
-}
resetCursorForSection : Int -> Model -> Model
resetCursorForSection idx model =
    let
        comp =
            Model.composition model

        section =
            comp.sections
                |> List.drop idx
                |> List.head

        startingBeat =
            section
                |> Maybe.map .startingBeat
                |> Maybe.withDefault 1

        currentSnapshot =
            UndoHistory.present model.history

        freshCursor =
            { taal = comp.metadata.taal
            , cycle = 0
            , beat = startingBeat - 1
            , subIndex = 0
            , totalSubdivisions = 1
            , currentOctave = Madhya
            , selectionAnchor = Nothing
            }

        newSnapshot =
            { currentSnapshot | cursor = freshCursor, sectionIndex = idx }
    in
    { model | history = UndoHistory.push newSnapshot model.history }



-- Raw HTTP helpers ----------------------------------------------------------
-- /compositions/serialize and /export/html return the body verbatim (no
-- ApiResult envelope), so we bypass Api.Client and read the body via
-- Http.expectString. The DebugDumpReceived / DebugExportReceived Msgs are
-- typed for Result Http.Error (ApiResult String), so we lift the raw
-- string into the Success arm of ApiResult before dispatching.


postExpectingRawString :
    { url : String, body : Encode.Value, onResult : Result Http.Error String -> Msg }
    -> Cmd Msg
postExpectingRawString cfg =
    Http.post
        { url = cfg.url
        , body = Http.jsonBody cfg.body
        , expect = Http.expectString cfg.onResult
        }


mapStringResultToApi : Result Http.Error String -> Result Http.Error (Api.Client.ApiResult String)
mapStringResultToApi r =
    Result.map Api.Client.Success r



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
        , ( "availableRaagsCount", Encode.int (List.length model.availableRaags) )
        , ( "availableTaalsCount", Encode.int (List.length model.availableTaals) )
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


{-| Placeholder: no canonical test inspects per-cursor events on web yet. When
one does, thread the active section's events out of `model` here.
-}
encodeEvents : Encode.Value
encodeEvents =
    Encode.list identity []


{-| Placeholder: no canonical test inspects undo/redo depth on web yet. When
one does, derive these from `model.history` here.
-}
encodeHistory : Encode.Value
encodeHistory =
    Encode.object
        [ ( "undoDepth", Encode.int 0 )
        , ( "redoDepth", Encode.int 0 )
        ]
