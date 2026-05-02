module State.Update exposing (update)

import Api.Client exposing (ApiResult(..))
import Api.Composition as ApiComposition
import Http
import Api.Cursor as ApiCursor
import Api.Editor as ApiEditor
import Api.Export as ApiExport
import Api.Layout as ApiLayout
import Api.Ornament as ApiOrnament
import Api.Playback as ApiPlayback
import Api.Reference as ApiReference
import Api.Section as ApiSection
import Api.Stroke as ApiStroke
import Input.KeyHandler as KeyHandler exposing (KeyAction(..))
import Input.OrnamentMode as OrnamentMode exposing (OrnamentAction(..))
import Json.Encode as Encode
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (EditorResult, LayoutConfig, SectionGrid)
import Model.Types
    exposing
        ( Laya(..)
        , MeendDirection(..)
        , Note(..)
        , NoteRef
        , Octave(..)
        , Stroke(..)
        , SwarScript(..)
        , Variant(..)
        )
import State.Model as Model
    exposing
        ( EditMode(..)
        , Model
        , OrnamentMode(..)
        , PlaybackState(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory exposing (Snapshot)
import Time


{-| Double-tap threshold in milliseconds.
-}
doubleTapThresholdMs : Int
doubleTapThresholdMs =
    350


{-| Main update function handling all Msg variants.
-}
update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        -- Keyboard input
        KeyPressed key shiftKey ctrlKey altKey ->
            handleKeyPress key shiftKey ctrlKey altKey model

        -- Mouse click on canvas
        CanvasClicked cycle beat ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.moveTo model.apiBaseUrl cur cycle beat GotCursorResult
            )

        -- File operations
        NewComposition ->
            ( { model | showNewDialog = True }, Cmd.none )

        OpenFile ->
            -- Handled via ports (Task 19-22)
            ( model, Cmd.none )

        SaveFile ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiComposition.serializeComposition model.apiBaseUrl comp GotSerializedComposition
            )

        ExportPdf ->
            -- PDF export is handled via ports for binary download
            ( addLog "PDF export initiated (requires port)" model, Cmd.none )

        ExportHtml ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiExport.exportHtml model.apiBaseUrl comp model.currentScript GotExportHtml
            )

        -- Edit operations
        Undo ->
            handleUndo model

        Redo ->
            handleRedo model

        -- Script
        ChangeScript script ->
            ( { model | currentScript = script }
                |> addLog ("Script changed to " ++ scriptName script)
            , Cmd.none
            )

        -- Section operations
        SelectSection idx ->
            ( { model | currentSectionIndex = idx }
                |> addLog ("Switched to section " ++ String.fromInt (idx + 1))
            , requestLayout model
            )

        AddSection name sectionType ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.addSection model.apiBaseUrl comp name sectionType GotSectionAdd
            )

        RemoveSection idx ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.removeSection model.apiBaseUrl comp model.currentSectionIndex idx GotSectionRemove
            )

        RenameSection idx newName ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiSection.renameSection model.apiBaseUrl comp idx newName GotSectionRename
            )

        MoveSectionUp idx ->
            if idx > 0 then
                let
                    comp =
                        Model.composition model
                in
                ( { model | pendingApiCall = True }
                , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx - 1) GotSectionReorder
                )

            else
                ( model, Cmd.none )

        MoveSectionDown idx ->
            let
                comp =
                    Model.composition model

                maxIdx =
                    List.length comp.sections - 1
            in
            if idx < maxIdx then
                ( { model | pendingApiCall = True }
                , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx + 1) GotSectionReorder
                )

            else
                ( model, Cmd.none )

        -- Playback
        Play ->
            let
                comp =
                    Model.composition model
            in
            ( { model | playbackState = Playing, pendingApiCall = True }
            , ApiPlayback.schedulePlayback model.apiBaseUrl comp model.bpm GotPlaybackSchedule
            )

        Pause ->
            ( { model | playbackState = Paused }
                |> addLog "Playback paused"
            , Cmd.none
            )

        Stop ->
            ( { model | playbackState = Stopped }
                |> addLog "Playback stopped"
            , Cmd.none
            )

        SetBpm bpm ->
            ( { model | bpm = bpm }, Cmd.none )

        ToggleLoop ->
            ( { model | loopEnabled = not model.loopEnabled }, Cmd.none )

        -- View toggles
        ToggleStrokeLine ->
            ( model, Cmd.none )

        ToggleSahityaLine ->
            ( model, Cmd.none )

        ToggleKeyboardLegend ->
            ( { model | showKeyboardLegend = not model.showKeyboardLegend }, Cmd.none )

        -- New dialog
        ShowNewDialog ->
            ( { model | showNewDialog = True }, Cmd.none )

        NewDialogSetTitle t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | title = t } }, Cmd.none )

        NewDialogSetType t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | compositionType = t } }, Cmd.none )

        NewDialogSetRaag r ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | raagName = r } }, Cmd.none )

        NewDialogSetTaal t ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | taalName = t } }, Cmd.none )

        NewDialogSetLaya l ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | layaName = l } }, Cmd.none )

        NewDialogSetTaanCount s ->
            let
                form =
                    model.newDialogForm

                count =
                    String.toInt s |> Maybe.withDefault 0
            in
            ( { model | newDialogForm = { form | taanCount = count } }, Cmd.none )

        NewDialogSetShowStrokes b ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | showStrokes = b } }, Cmd.none )

        NewDialogSetShowSahitya b ->
            let
                form =
                    model.newDialogForm
            in
            ( { model | newDialogForm = { form | showSahitya = b } }, Cmd.none )

        NewDialogSubmit ->
            handleNewDialogSubmit model

        NewDialogCancel ->
            ( { model | showNewDialog = False }, Cmd.none )

        -- Properties dialog
        ShowPropsDialog ->
            let
                comp =
                    Model.composition model
            in
            ( { model
                | showPropsDialog = True
                , propsDialogForm =
                    { title = comp.metadata.title
                    , taalName = String.toLower comp.metadata.taal.name
                    }
              }
            , Cmd.none
            )

        PropsDialogSetTitle t ->
            let
                form =
                    model.propsDialogForm
            in
            ( { model | propsDialogForm = { form | title = t } }, Cmd.none )

        PropsDialogSetTaal t ->
            let
                form =
                    model.propsDialogForm
            in
            ( { model | propsDialogForm = { form | taalName = t } }, Cmd.none )

        PropsDialogSubmit ->
            ( { model | showPropsDialog = False }
                |> addLog "Properties updated"
            , Cmd.none
            )

        PropsDialogCancel ->
            ( { model | showPropsDialog = False }, Cmd.none )

        -- About dialog
        ShowAboutDialog ->
            ( { model | showAboutDialog = True }, Cmd.none )

        CloseAboutDialog ->
            ( { model | showAboutDialog = False }, Cmd.none )

        -- API Responses
        GotEditorResult result ->
            handleEditorApiResult result model

        GotCursorResult result ->
            handleCursorApiResult result model

        GotLayoutResult result ->
            handleLayoutApiResult result model

        GotTaals result ->
            handleApiResult result
                (\taals ->
                    ( { model | availableTaals = taals }
                        |> addLog ("Loaded " ++ String.fromInt (List.length taals) ++ " taals")
                    , Cmd.none
                    )
                )
                model

        GotRaags result ->
            handleApiResult result
                (\raags ->
                    ( { model | availableRaags = raags }
                        |> addLog ("Loaded " ++ String.fromInt (List.length raags) ++ " raags")
                    , Cmd.none
                    )
                )
                model

        GotColors result ->
            handleApiResult result
                (\colors ->
                    ( { model | notationColors = Just colors }
                        |> addLog "Colors loaded"
                    , Cmd.none
                    )
                )
                model

        GotScripts result ->
            handleApiResult result
                (\scripts ->
                    ( { model | availableScripts = scripts }, Cmd.none )
                )
                model

        GotNewComposition result ->
            handleApiResult result
                (\comp ->
                    let
                        defaultCursor =
                            { taal = comp.metadata.taal
                            , cycle = 0
                            , beat = 0
                            , subIndex = 0
                            , totalSubdivisions = 1
                            , currentOctave = Madhya
                            }

                        snapshot =
                            { composition = comp
                            , cursor = defaultCursor
                            , sectionIndex = 0
                            }

                        newModel =
                            { model
                                | history = UndoHistory.init snapshot
                                , currentSectionIndex = 0
                                , showNewDialog = False
                                , pendingApiCall = False
                            }
                                |> addLog ("Created: " ++ comp.metadata.title)
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionAdd result ->
            handleApiResult result
                (\comp ->
                    let
                        newModel =
                            updateComposition comp model
                                |> addLog "Section added"
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionRemove result ->
            handleApiResult result
                (\r ->
                    let
                        newModel =
                            updateComposition r.composition model
                                |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                                |> addLog "Section removed"
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionRename result ->
            handleApiResult result
                (\comp ->
                    let
                        newModel =
                            updateComposition comp model
                                |> addLog "Section renamed"
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotSectionReorder result ->
            handleApiResult result
                (\r ->
                    let
                        newModel =
                            updateComposition r.composition model
                                |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                                |> addLog "Sections reordered"
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        GotPlaybackSchedule result ->
            handleApiResult result
                (\timedNotes ->
                    -- Playback notes will be sent to JS via ports (Task 19-22)
                    ( { model | pendingApiCall = False }
                        |> addLog ("Playback scheduled: " ++ String.fromInt (List.length timedNotes) ++ " notes")
                    , Cmd.none
                    )
                )
                model

        GotExportHtml result ->
            handleApiResult result
                (\htmlString ->
                    -- HTML export will be handled via ports
                    ( { model | pendingApiCall = False }
                        |> addLog "HTML export ready"
                    , Cmd.none
                    )
                )
                model

        GotSerializedComposition result ->
            handleApiResult result
                (\jsonValue ->
                    -- Serialized JSON will be sent to JS via ports for file save
                    ( { model | pendingApiCall = False }
                        |> addLog "Composition serialized"
                    , Cmd.none
                    )
                )
                model

        GotParsedComposition result ->
            handleApiResult result
                (\comp ->
                    let
                        defaultCursor =
                            { taal = comp.metadata.taal
                            , cycle = 0
                            , beat = 0
                            , subIndex = 0
                            , totalSubdivisions = 1
                            , currentOctave = Madhya
                            }

                        snapshot =
                            { composition = comp
                            , cursor = defaultCursor
                            , sectionIndex = 0
                            }

                        newModel =
                            { model
                                | history = UndoHistory.init snapshot
                                , currentSectionIndex = 0
                                , pendingApiCall = False
                            }
                                |> addLog ("Opened: " ++ comp.metadata.title)
                    in
                    ( newModel, requestLayout newModel )
                )
                model

        -- File port responses
        FileSelected filename ->
            ( addLog ("File selected: " ++ filename) model, Cmd.none )

        FileLoaded content ->
            ( { model | pendingApiCall = True }
            , ApiComposition.parseComposition model.apiBaseUrl content GotParsedComposition
            )

        -- Timers
        CursorBlink _ ->
            ( { model | cursorVisible = not model.cursorVisible }, Cmd.none )

        Tick posix ->
            ( model, Cmd.none )

        -- No-op
        NoOp ->
            ( model, Cmd.none )



-- KEYBOARD HANDLING


handleKeyPress : String -> Bool -> Bool -> Bool -> Model -> ( Model, Cmd Msg )
handleKeyPress key shiftKey ctrlKey altKey model =
    let
        action =
            KeyHandler.mapKeyToAction key shiftKey ctrlKey altKey
    in
    -- Check if we are in an ornament mode first
    case model.ornamentMode of
        NoOrnament ->
            handleKeyAction action key model

        _ ->
            handleOrnamentInput action model


handleKeyAction : KeyAction -> String -> Model -> ( Model, Cmd Msg )
handleKeyAction action key model =
    case action of
        SwarInput note variant ->
            handleSwarKey note variant key model

        InsertRest ->
            let
                comp =
                    Model.composition model

                cur =
                    Model.cursor model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.insertRest model.apiBaseUrl comp model.currentSectionIndex cur GotEditorResult
            )

        InsertSustain ->
            let
                comp =
                    Model.composition model

                cur =
                    Model.cursor model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.insertSustain model.apiBaseUrl comp model.currentSectionIndex cur GotEditorResult
            )

        DeleteLast ->
            let
                comp =
                    Model.composition model

                cur =
                    Model.cursor model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.deleteLast model.apiBaseUrl comp model.currentSectionIndex cur GotEditorResult
            )

        NavRight ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.nextBeat model.apiBaseUrl cur GotCursorResult
            )

        NavLeft ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.prevBeat model.apiBaseUrl cur GotCursorResult
            )

        NavNextSubBeat ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.nextSubBeat model.apiBaseUrl cur GotCursorResult
            )

        UndoAction ->
            handleUndo model

        RedoAction ->
            handleRedo model

        Subdivision n ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.setSubdivisions model.apiBaseUrl cur n GotCursorResult
            )

        OctaveMandra ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.setOctave model.apiBaseUrl cur Mandra GotCursorResult
            )

        OctaveMadhya ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.setOctave model.apiBaseUrl cur Madhya GotCursorResult
            )

        OctaveTaar ->
            let
                cur =
                    Model.cursor model
            in
            ( model
            , ApiCursor.setOctave model.apiBaseUrl cur Taar GotCursorResult
            )

        StrokeDa ->
            handleStroke Da model

        StrokeRa ->
            handleStroke Ra model

        StrokeChikari ->
            handleStroke Chikari model

        StrokeJod ->
            handleStroke Jod model

        StrokeClear ->
            let
                comp =
                    Model.composition model

                cur =
                    Model.cursor model
            in
            ( { model | pendingApiCall = True }
            , ApiStroke.clearStroke model.apiBaseUrl comp model.currentSectionIndex cur GotEditorResult
            )

        -- Ornament shortcuts: enter ornament mode
        OrnamentGamak ->
            applySimpleOrnament "gamak" model

        OrnamentAndolan ->
            applySimpleOrnament "andolan" model

        OrnamentGitkari ->
            applySimpleOrnament "gitkari" model

        OrnamentKanSwar ->
            ( { model | ornamentMode = SingleNoteMode "kanSwar" }
                |> addLog "Kan Swar: type the grace note"
            , Cmd.none
            )

        OrnamentSparsh ->
            ( { model | ornamentMode = SingleNoteMode "sparsh" }
                |> addLog "Sparsh: type the touch note"
            , Cmd.none
            )

        OrnamentGhaseet ->
            ( { model | ornamentMode = SingleNoteMode "ghaseet" }
                |> addLog "Ghaseet: type the target note"
            , Cmd.none
            )

        OrnamentMeendAsc ->
            ( { model | ornamentMode = MeendStartMode Ascending }
                |> addLog "Meend (ascending): type start note"
            , Cmd.none
            )

        OrnamentMeendDesc ->
            ( { model | ornamentMode = MeendStartMode Descending }
                |> addLog "Meend (descending): type start note"
            , Cmd.none
            )

        OrnamentKrintan ->
            ( { model | ornamentMode = KrintanStartMode }
                |> addLog "Krintan: type notes, then Enter"
            , Cmd.none
            )

        OrnamentMurki ->
            ( { model | ornamentMode = MurkiCollectMode [] }
                |> addLog "Murki: type notes, then Enter"
            , Cmd.none
            )

        OrnamentZamzama ->
            ( { model | ornamentMode = ZamzamaCollectMode [] }
                |> addLog "Zamzama: type notes, then Enter"
            , Cmd.none
            )

        OrnamentCancel ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog "Ornament mode cancelled"
            , Cmd.none
            )

        ToggleEditMode ->
            let
                newMode =
                    case model.editMode of
                        SwarEdit ->
                            StrokeEdit

                        StrokeEdit ->
                            SwarEdit
            in
            ( { model | editMode = newMode }
                |> addLog
                    ("Edit mode: "
                        ++ (case newMode of
                                SwarEdit ->
                                    "Swar"

                                StrokeEdit ->
                                    "Stroke"
                           )
                    )
            , Cmd.none
            )

        NoAction ->
            ( model, Cmd.none )


{-| Handle swar key input with double-tap detection.
-}
handleSwarKey : Note -> Variant -> String -> Model -> ( Model, Cmd Msg )
handleSwarKey note variant key model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        now =
            model.lastTypedTime

        isDoubleTap =
            model.lastTypedChar == key && now > 0
    in
    if isDoubleTap then
        -- Double-tap: insert dual swar
        ( { model
            | pendingApiCall = True
            , lastTypedChar = ""
            , lastTypedTime = 0
          }
        , ApiEditor.insertDualSwar model.apiBaseUrl comp model.currentSectionIndex cur note variant cur.currentOctave GotEditorResult
        )

    else
        -- Single tap: insert swar
        ( { model
            | pendingApiCall = True
            , lastTypedChar = key
            , lastTypedTime = 1
          }
        , ApiEditor.insertSwar model.apiBaseUrl comp model.currentSectionIndex cur note variant cur.currentOctave GotEditorResult
        )


{-| Handle ornament input when in an ornament mode.
-}
handleOrnamentInput : KeyAction -> Model -> ( Model, Cmd Msg )
handleOrnamentInput action model =
    let
        maybeNoteRef =
            case action of
                SwarInput note variant ->
                    Just
                        { note = note
                        , variant = variant
                        , octave = (Model.cursor model).currentOctave
                        }

                _ ->
                    Nothing

        isEnter =
            case action of
                NoAction ->
                    False

                _ ->
                    False

        -- Check if the raw action was triggered by Enter
        -- We handle this via the OrnamentCancel action check below
        ornamentAction =
            OrnamentMode.transition model.ornamentMode maybeNoteRef isEnter
    in
    case action of
        OrnamentCancel ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog "Ornament mode cancelled"
            , Cmd.none
            )

        _ ->
            applyOrnamentAction ornamentAction model


applyOrnamentAction : OrnamentAction -> Model -> ( Model, Cmd Msg )
applyOrnamentAction action model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        secIdx =
            model.currentSectionIndex
    in
    case action of
        ApplySimple ornamentType ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addSimple model.apiBaseUrl comp secIdx cur ornamentType GotEditorResult
            )

        ApplySingleNote ornamentType noteRef ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addSingleNote model.apiBaseUrl comp secIdx cur ornamentType noteRef GotEditorResult
            )

        ApplyMeend startNote endNote direction ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addMeend model.apiBaseUrl
                comp
                secIdx
                cur
                { startNote = startNote
                , endNote = endNote
                , direction = direction
                , intermediateNotes = []
                }
                GotEditorResult
            )

        ApplyKrintan notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addKrintan model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        ApplyMurki notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addMurki model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        ApplyZamzama notes ->
            ( { model | ornamentMode = NoOrnament, pendingApiCall = True }
            , ApiOrnament.addZamzama model.apiBaseUrl comp secIdx cur notes GotEditorResult
            )

        StillCollecting newMode ->
            ( { model | ornamentMode = newMode }
                |> addLog "Collecting ornament notes..."
            , Cmd.none
            )

        Cancelled ->
            ( { model | ornamentMode = NoOrnament }
                |> addLog "Ornament cancelled"
            , Cmd.none
            )


applySimpleOrnament : String -> Model -> ( Model, Cmd Msg )
applySimpleOrnament ornamentType model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiOrnament.addSimple model.apiBaseUrl comp model.currentSectionIndex cur ornamentType GotEditorResult
    )


handleStroke : Stroke -> Model -> ( Model, Cmd Msg )
handleStroke stroke model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiStroke.setStroke model.apiBaseUrl comp model.currentSectionIndex cur stroke GotEditorResult
    )



-- UNDO / REDO


handleUndo : Model -> ( Model, Cmd Msg )
handleUndo model =
    case UndoHistory.undo model.history of
        Just newHistory ->
            let
                newModel =
                    { model
                        | history = newHistory
                        , currentSectionIndex = (UndoHistory.present newHistory).sectionIndex
                    }
                        |> addLog "Undo"
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog "Nothing to undo" model, Cmd.none )


handleRedo : Model -> ( Model, Cmd Msg )
handleRedo model =
    case UndoHistory.redo model.history of
        Just newHistory ->
            let
                newModel =
                    { model
                        | history = newHistory
                        , currentSectionIndex = (UndoHistory.present newHistory).sectionIndex
                    }
                        |> addLog "Redo"
            in
            ( newModel, requestLayout newModel )

        Nothing ->
            ( addLog "Nothing to redo" model, Cmd.none )



-- API RESPONSE HANDLERS


handleEditorApiResult : Result Http.Error (ApiResult EditorResult) -> Model -> ( Model, Cmd Msg )
handleEditorApiResult result model =
    handleApiResult result
        (\editorResult ->
            let
                snapshot =
                    { composition = editorResult.composition
                    , cursor = editorResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
                        |> addLog editorResult.message
            in
            ( newModel, requestLayout newModel )
        )
        model


handleCursorApiResult : Result Http.Error (ApiResult CursorModel) -> Model -> ( Model, Cmd Msg )
handleCursorApiResult result model =
    handleApiResult result
        (\newCursor ->
            let
                currentSnapshot =
                    UndoHistory.present model.history

                snapshot =
                    { currentSnapshot | cursor = newCursor }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                    }
            in
            ( newModel, Cmd.none )
        )
        model


handleLayoutApiResult : Result Http.Error (ApiResult (List SectionGrid)) -> Model -> ( Model, Cmd Msg )
handleLayoutApiResult result model =
    handleApiResult result
        (\grids ->
            ( { model | layoutGrids = grids, pendingApiCall = False }, Cmd.none )
        )
        model


{-| Generic API result handler that extracts Success, logs ApiFailure/HttpError.
-}
handleApiResult :
    Result Http.Error (ApiResult a)
    -> (a -> ( Model, Cmd Msg ))
    -> Model
    -> ( Model, Cmd Msg )
handleApiResult result onSuccess model =
    case result of
        Ok (Success data) ->
            onSuccess data

        Ok (ApiFailure apiError) ->
            ( { model | pendingApiCall = False }
                |> addLog ("API error: " ++ apiError.message)
            , Cmd.none
            )

        Err httpError ->
            ( { model | pendingApiCall = False }
                |> addLog ("HTTP error: " ++ httpErrorToString httpError)
            , Cmd.none
            )


updateComposition : Composition -> Model -> Model
updateComposition comp model =
    let
        currentSnapshot =
            UndoHistory.present model.history

        snapshot =
            { currentSnapshot | composition = comp }
    in
    { model
        | history = UndoHistory.push snapshot model.history
        , pendingApiCall = False
    }



-- NEW DIALOG SUBMISSION


handleNewDialogSubmit : Model -> ( Model, Cmd Msg )
handleNewDialogSubmit model =
    let
        form =
            model.newDialogForm

        maybeTaal =
            findByName form.taalName model.availableTaals

        maybeRaag =
            findByName form.raagName model.availableRaags
    in
    case ( maybeTaal, maybeRaag ) of
        ( Just taal, Just raag ) ->
            let
                compType =
                    case form.compositionType of
                        "bandish" ->
                            Bandish

                        "palta" ->
                            Palta

                        _ ->
                            Gat

                laya =
                    case form.layaName of
                        "ativilambit" ->
                            Just AtiVilambit

                        "vilambit" ->
                            Just Vilambit

                        "madhya" ->
                            Just MadhyaLaya

                        "drut" ->
                            Just Drut

                        "atidrut" ->
                            Just AtiDrut

                        _ ->
                            Nothing
            in
            ( { model | pendingApiCall = True }
            , ApiComposition.createComposition model.apiBaseUrl
                { title = form.title
                , compositionType = compType
                , taal = taal
                , raag = raag
                , laya = laya
                , taanCount = form.taanCount
                , showStrokeLine = form.showStrokes
                , showSahityaLine = form.showSahitya
                }
                GotNewComposition
            )

        _ ->
            ( addLog "Please select a valid taal and raag" model, Cmd.none )



-- HELPERS


{-| Request layout computation from the server.
-}
requestLayout : Model -> Cmd Msg
requestLayout model =
    let
        comp =
            Model.composition model
    in
    ApiLayout.computeLayout model.apiBaseUrl comp Model.defaultLayoutConfig GotLayoutResult


{-| Add a log entry to the status log (newest first), capped at 100 entries.
-}
addLog : String -> Model -> Model
addLog message model =
    { model | statusLog = List.take 100 (message :: model.statusLog) }


findByName : String -> List ( String, a ) -> Maybe a
findByName name pairs =
    pairs
        |> List.filter (\( n, _ ) -> String.toLower n == String.toLower name)
        |> List.head
        |> Maybe.map Tuple.second


scriptName : SwarScript -> String
scriptName script =
    case script of
        Devanagari ->
            "Devanagari"

        Kannada ->
            "Kannada"

        Telugu ->
            "Telugu"

        English ->
            "English"


httpErrorToString : Http.Error -> String
httpErrorToString error =
    case error of
        Http.BadUrl url ->
            "Bad URL: " ++ url

        Http.Timeout ->
            "Request timed out"

        Http.NetworkError ->
            "Network error"

        Http.BadStatus code ->
            "Bad status: " ++ String.fromInt code

        Http.BadBody msg ->
            "Bad body: " ++ msg
