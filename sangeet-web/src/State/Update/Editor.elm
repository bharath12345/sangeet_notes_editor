module State.Update.Editor exposing
    ( handleCanvasClicked
    , handleChangeScript
    , handleClipboardContentReceived
    , handleGotClipboardResult
    , handleGotSwarKeyTime
    , handleKeyPress
    , handleRedo
    , handleToggleTheme
    , handleUndo
    )

{-| Editor handlers: keyboard routing, swar input (with fast-typing
grouping), ornament mode, cursor navigation, selection/clipboard, undo/
redo, theme toggle, and script switching. The keyboard router dispatches
into a private `handleKeyAction` that also covers cursor moves, stroke
edit, and ornament shortcuts.

`handleKeyPress`, `handleUnsavedChangesSave`-style flows recursively call
the top-level `update`. To avoid an import cycle with State.Update, the
recursive entry point is passed in as `runUpdate`.

-}

import Api.Client exposing (ApiResult)
import Api.Cursor as ApiCursor
import Api.Editor as ApiEditor
import Api.Ornament as ApiOrnament
import Api.Stroke as ApiStroke
import Http
import Input.KeyHandler as KeyHandler exposing (KeyAction(..))
import Input.OrnamentMode as OrnamentMode exposing (OrnamentAction(..))
import Model.Composition exposing (SectionType(..))
import Model.Layout exposing (ClipboardResult)
import Model.Types
    exposing
        ( MeendDirection(..)
        , Note
        , Octave(..)
        , Stroke(..)
        , SwarScript
        , Variant
        )
import Ports
import State.Model as Model
    exposing
        ( EditMode(..)
        , Model
        , OrnamentMode(..)
        , Theme(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Helpers as Helpers
import Task
import Time
import UiStrings


{-| Grouping threshold in milliseconds — notes typed within this window
on the same beat are grouped onto a single beat with equal subdivisions.
-}
groupingThresholdMs : Int
groupingThresholdMs =
    500



-- THEME / SCRIPT


handleChangeScript : SwarScript -> Model -> ( Model, Cmd Msg )
handleChangeScript script model =
    ( { model | currentScript = script }
        |> Helpers.addLog (UiStrings.statusScriptChanged |> String.replace "{scriptName}" (Helpers.scriptName script))
    , Cmd.none
    )


handleToggleTheme : Model -> ( Model, Cmd Msg )
handleToggleTheme model =
    let
        next =
            case model.theme of
                Light ->
                    Dark

                Dark ->
                    Light

        nextName =
            Model.themeName next
    in
    ( { model | theme = next }
        |> Helpers.addLog (UiStrings.statusThemeChanged |> String.replace "{themeName}" nextName)
    , Ports.setTheme nextName
    )



-- MOUSE


handleCanvasClicked : Int -> Int -> Model -> ( Model, Cmd Msg )
handleCanvasClicked cycle beat model =
    let
        cur =
            Model.cursor model
    in
    ( { model | groupingState = Nothing }
    , ApiCursor.moveTo model.apiBaseUrl cur cycle beat (Model.currentStartingBeat model) GotCursorResult
    )



-- KEYBOARD HANDLING


handleKeyPress : (Msg -> Model -> ( Model, Cmd Msg )) -> String -> Bool -> Bool -> Bool -> Model -> ( Model, Cmd Msg )
handleKeyPress runUpdate key shiftKey ctrlKey altKey model =
    -- Palette has its own key handling: ↑/↓ navigate, Enter runs, Esc closes.
    -- Any other key (including text input into the search field) falls through
    -- so the input element processes it naturally.
    if model.showCommandPalette then
        case key of
            "Escape" ->
                runUpdate CloseCommandPalette model

            "ArrowDown" ->
                runUpdate (PaletteSelectIndex (model.paletteSelectedIndex + 1)) model

            "ArrowUp" ->
                runUpdate (PaletteSelectIndex (model.paletteSelectedIndex - 1)) model

            "Enter" ->
                runUpdate PaletteRunSelected model

            _ ->
                ( model, Cmd.none )
        -- Ctrl/Cmd+K opens the palette. Alt+K is taken by the kanSwar ornament.

    else
        let
            anyDialogOpen =
                model.showNewDialog
                    || model.showPropsDialog
                    || model.showAboutDialog
                    || model.showBugReportDialog
                    || model.showKeyboardCheatSheet
                    || model.showSupportDialog
                    || model.showDuplicateTabDialog
                    || model.showUnsavedChangesDialog
                    /= Nothing
        in
        -- Ctrl/Cmd+K opens the palette. Alt+K is taken by the kanSwar ornament.
        if ctrlKey && not altKey && not shiftKey && key == "k" && not anyDialogOpen then
            runUpdate ShowCommandPalette model
            -- Bare `?` opens the cheat sheet — guarded so it doesn't fire while the
            -- user is typing into a dialog text field or in ornament mode.

        else if key == "?" && not ctrlKey && not altKey && not anyDialogOpen && model.ornamentMode == NoOrnament then
            runUpdate ShowKeyboardCheatSheet model
            -- Ctrl+Shift+S → Save As (browser doesn't reserve this combo).
            -- Mirrors desktop MainApp.scala:437. Browsers leave both `s` and `S`
            -- through, but the `key` value follows shift-state — match both.

        else if ctrlKey && shiftKey && not altKey && (key == "S" || key == "s") && not anyDialogOpen then
            runUpdate SaveFileAs model
            -- Ctrl+S → Save. Mirrors desktop MainApp.scala:435.
            -- The browser's "Save Page As" default (Ctrl+S without shift) is
            -- intercepted in ports.js via a document-level keydown listener
            -- with preventDefault. The Elm subscription still sees the event
            -- (we don't stopPropagation), so SaveFile fires here normally.

        else if ctrlKey && not shiftKey && not altKey && key == "s" && not anyDialogOpen then
            runUpdate SaveFile model
            -- Ctrl+, → Edit composition properties. Mirrors desktop MainApp.scala:443.

        else if ctrlKey && not shiftKey && not altKey && key == "," && not anyDialogOpen then
            runUpdate ShowPropsDialog model
            -- Ctrl+Shift+A → Add section. Mirrors desktop MainApp.scala:445.
            -- Inserts a Taan section with the default name; user can rename via
            -- the section chip or the Rename palette action.

        else if ctrlKey && shiftKey && not altKey && (key == "A" || key == "a") && not anyDialogOpen then
            runUpdate (AddSection UiStrings.actionAddSectionDefaultName Taan) model
            -- Ctrl+Shift+Backspace → Remove current section.
            -- Mirrors desktop MainApp.scala:449.

        else if ctrlKey && shiftKey && not altKey && key == "Backspace" && not anyDialogOpen then
            runUpdate (RemoveSection model.currentSectionIndex) model
            -- Ctrl+Z → Undo. Mirrors desktop MainApp.scala:426.
            -- Also handles Cmd+Z on macOS (browsers map metaKey to ctrlKey on Mac).
            -- Skip if a dialog is open to avoid interfering with native undo in text fields.

        else if ctrlKey && not shiftKey && not altKey && key == "z" && not anyDialogOpen then
            runUpdate Undo model
            -- Ctrl+Shift+Z → Redo. Mirrors desktop MainApp.scala:428 (though desktop
            -- uses Ctrl+Y as primary). Also handles Cmd+Shift+Z on macOS.

        else if ctrlKey && shiftKey && not altKey && (key == "Z" || key == "z") && not anyDialogOpen then
            runUpdate Redo model

        else
            let
                action =
                    KeyHandler.mapKeyToAction key shiftKey ctrlKey altKey
            in
            case model.ornamentMode of
                NoOrnament ->
                    handleKeyAction action key model

                _ ->
                    handleOrnamentInput action model


handleKeyAction : KeyAction -> String -> Model -> ( Model, Cmd Msg )
handleKeyAction action key model =
    let
        -- Clear grouping state for any action other than SwarInput in SwarEdit mode
        m =
            case ( action, model.editMode ) of
                ( SwarInput _ _, SwarEdit ) ->
                    model

                _ ->
                    { model | groupingState = Nothing }
    in
    case action of
        SwarInput note variant ->
            case m.editMode of
                StrokeEdit ->
                    case String.toLower key of
                        "d" ->
                            handleStroke Da m

                        "r" ->
                            handleStroke Ra m

                        "j" ->
                            handleStroke Jod m

                        _ ->
                            ( m, Cmd.none )

                SwarEdit ->
                    handleSwarKey note variant key m

        InsertRest ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertRest m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        InsertSustain ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertSustain m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        InsertChikari ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.insertChikari m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        DeleteLast ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiEditor.deleteAtCursor m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        NavRight ->
            let
                cur =
                    Model.cursor m

                cleared =
                    { cur | selectionAnchor = Nothing }

                -- Match desktop's clamp: NavRight is a no-op once the
                -- cursor is already at the "one cycle past the last
                -- event" position. Otherwise the cursor advances into a
                -- cycle that has no rendered cells and visually
                -- disappears (plan-16 B.5a). Server-side nextBeat would
                -- still happily advance, so we clamp here before firing.
                maxAllowedCycle =
                    Model.currentSectionMaxCycle m + 1

                taal =
                    cleared.taal

                wouldOverflowCycle =
                    cleared.beat + 1 >= taal.matras
            in
            if wouldOverflowCycle && cleared.cycle >= maxAllowedCycle then
                ( m, Cmd.none )

            else
                ( Helpers.updateCursorInPlace cleared m
                , ApiCursor.nextBeat m.apiBaseUrl cleared (Model.currentStartingBeat m) GotCursorResult
                )

        NavLeft ->
            let
                cur =
                    Model.cursor m

                cleared =
                    { cur | selectionAnchor = Nothing }
            in
            ( Helpers.updateCursorInPlace cleared m
            , ApiCursor.prevBeat m.apiBaseUrl cleared (Model.currentStartingBeat m) GotCursorResult
            )

        NavNextSubBeat ->
            let
                cur =
                    Model.cursor m
            in
            ( m
            , ApiCursor.nextSubBeat m.apiBaseUrl cur (Model.currentStartingBeat m) GotCursorResult
            )

        UndoAction ->
            handleUndo m

        RedoAction ->
            handleRedo m

        Subdivision n ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setSubdivisions m.apiBaseUrl cur n GotCursorResult
            )

        OctaveMandra ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Mandra GotCursorResult
            )

        OctaveMadhya ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Madhya GotCursorResult
            )

        OctaveTaar ->
            let
                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiCursor.setOctave m.apiBaseUrl cur Taar GotCursorResult
            )

        StrokeDa ->
            handleStroke Da m

        StrokeRa ->
            handleStroke Ra m

        StrokeJod ->
            handleStroke Jod m

        StrokeClear ->
            let
                comp =
                    Model.composition m

                cur =
                    Model.cursor m
            in
            ( { m | pendingApiCall = True }
            , ApiStroke.clearStroke m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
            )

        -- Ornament shortcuts: enter ornament mode
        OrnamentGamak ->
            applySimpleOrnament "gamak" m

        OrnamentAndolan ->
            applySimpleOrnament "andolan" m

        OrnamentGitkari ->
            applySimpleOrnament "gitkari" m

        OrnamentKanSwar ->
            ( { m | ornamentMode = SingleNoteMode "kanSwar" }
                |> Helpers.addLog UiStrings.statusOrnamentKanSwar
            , Cmd.none
            )

        OrnamentSparsh ->
            ( { m | ornamentMode = SingleNoteMode "sparsh" }
                |> Helpers.addLog UiStrings.statusOrnamentSparsh
            , Cmd.none
            )

        OrnamentGhaseet ->
            ( { m | ornamentMode = SingleNoteMode "ghaseet" }
                |> Helpers.addLog UiStrings.statusOrnamentGhaseet
            , Cmd.none
            )

        OrnamentMeendAsc ->
            ( { m | ornamentMode = MeendStartMode Ascending }
                |> Helpers.addLog UiStrings.statusOrnamentMeendAsc
            , Cmd.none
            )

        OrnamentMeendDesc ->
            ( { m | ornamentMode = MeendStartMode Descending }
                |> Helpers.addLog UiStrings.statusOrnamentMeendDesc
            , Cmd.none
            )

        OrnamentKrintan ->
            ( { m | ornamentMode = KrintanStartMode }
                |> Helpers.addLog UiStrings.statusOrnamentKrintan
            , Cmd.none
            )

        OrnamentMurki ->
            ( { m | ornamentMode = MurkiCollectMode [] }
                |> Helpers.addLog UiStrings.statusOrnamentMurki
            , Cmd.none
            )

        OrnamentZamzama ->
            ( { m | ornamentMode = ZamzamaCollectMode [] }
                |> Helpers.addLog UiStrings.statusOrnamentZamzama
            , Cmd.none
            )

        OrnamentCancel ->
            ( { m | ornamentMode = NoOrnament }
                |> Helpers.addLog UiStrings.statusOrnamentCancelled
            , Cmd.none
            )

        FinishOrnament ->
            ( m, Cmd.none )

        SelectRight ->
            handleSelectRight m

        SelectLeft ->
            handleSelectLeft m

        CopySelection ->
            handleCopySelection m

        CutSelection ->
            handleCutSelection m

        PasteClipboard ->
            ( m, Cmd.none )

        ToggleEditMode ->
            let
                newMode =
                    case m.editMode of
                        SwarEdit ->
                            StrokeEdit

                        StrokeEdit ->
                            SwarEdit
            in
            ( { m | editMode = newMode }
                |> Helpers.addLog
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
            case m.editMode of
                StrokeEdit ->
                    case String.toLower key of
                        "j" ->
                            handleStroke Jod m

                        "x" ->
                            let
                                comp =
                                    Model.composition m

                                cur =
                                    Model.cursor m
                            in
                            ( { m | pendingApiCall = True }
                            , ApiStroke.clearStroke m.apiBaseUrl comp m.currentSectionIndex cur GotEditorResult
                            )

                        _ ->
                            ( m, Cmd.none )

                SwarEdit ->
                    ( m, Cmd.none )


{-| Defer swar insertion until we have a timestamp for grouping detection.

We optimistically set pendingApiCall = True so the debug-bridge ack drain
waits until GotSwarKeyTime fires (and that handler in turn sets the flag
when it dispatches the actual API call). Without this, a fast-fire sequence
of debug TypeChar commands would resolve their acks before the Time.now
Task completes, losing the per-command sequencing the parity runner relies
on. Production keyboard handling sees no functional change — pendingApiCall
is only read by UI affordances that already gate on it being True for
brief windows after each key press.

-}
handleSwarKey : Note -> Variant -> String -> Model -> ( Model, Cmd Msg )
handleSwarKey note variant key model =
    ( { model | pendingApiCall = True }
    , Task.perform (\posix -> GotSwarKeyTime posix note variant key) Time.now
    )


{-| Handle swar input with timestamp — implements fast-typing grouping.
Notes typed within groupingThresholdMs on the same beat are accumulated
into a single beat via undo-and-replay with insertSwarGroup.

(Dispatched from the GotSwarKeyTime Msg branch; the `key` element of the
Msg is unused, hence the underscore in the dispatcher.)

-}
handleGotSwarKeyTime : Time.Posix -> Note -> Variant -> Model -> ( Model, Cmd Msg )
handleGotSwarKeyTime posix note variant model =
    let
        now =
            Time.posixToMillis posix

        cur =
            Model.cursor model

        octave =
            cur.currentOctave
    in
    case model.groupingState of
        Just gs ->
            -- Bug 4 fix: also require the current cursor to match where the
            -- previous insert advanced it to (gs.nextBeat/Cycle/SubIndex).
            -- If the user navigated between keystrokes, the current cursor
            -- has drifted and we must NOT collapse this keystroke onto the
            -- group's original beat.
            let
                cursorStillAlignedWithGroup =
                    cur.beat == gs.nextBeat && cur.cycle == gs.nextCycle && cur.subIndex == gs.nextSubIndex
            in
            if now - gs.startTime < groupingThresholdMs && List.length gs.notes < 4 && cursorStillAlignedWithGroup then
                case UndoHistory.undo model.history of
                    Just undoneHistory ->
                        let
                            thisNote =
                                { note = note, variant = variant, octave = octave }

                            undoneSnapshot =
                                UndoHistory.present undoneHistory

                            allNotes =
                                gs.notes ++ [ thisNote ]
                        in
                        ( { model
                            | history = undoneHistory
                            , pendingApiCall = True
                            , groupingState = Just { gs | notes = allNotes }
                          }
                        , ApiEditor.insertSwarGroup
                            model.apiBaseUrl
                            undoneSnapshot.composition
                            undoneSnapshot.sectionIndex
                            undoneSnapshot.cursor
                            allNotes
                            GotEditorResult
                        )

                    Nothing ->
                        startNewGroup model note variant octave now

            else
                startNewGroup model note variant octave now

        Nothing ->
            startNewGroup model note variant octave now


startNewGroup : Model -> Note -> Variant -> Octave -> Int -> ( Model, Cmd Msg )
startNewGroup model note variant octave now =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model

        thisNote =
            { note = note, variant = variant, octave = octave }
    in
    ( { model
        | pendingApiCall = True
        , groupingState =
            Just
                { notes = [ thisNote ]
                , startTime = now
                , beat = cur.beat
                , cycle = cur.cycle

                -- nextBeat/nextCycle/nextSubIndex are populated for real once
                -- the API response arrives (handleEditorApiResult overwrites
                -- them with the cursor the server advanced to). We seed them
                -- with the pre-insert position so that if the next keystroke
                -- arrives before the response settles, the cursor-alignment
                -- check correctly fails (we don't know yet where the cursor
                -- "should" be, so we can't safely extend the group).
                , nextBeat = cur.beat
                , nextCycle = cur.cycle
                , nextSubIndex = cur.subIndex
                }
      }
    , ApiEditor.insertSwar model.apiBaseUrl comp model.currentSectionIndex cur note variant octave GotEditorResult
    )


{-| Handle ornament input when in an ornament mode.
-}
handleOrnamentInput : KeyAction -> Model -> ( Model, Cmd Msg )
handleOrnamentInput action model =
    case action of
        OrnamentCancel ->
            ( { model | ornamentMode = NoOrnament }
                |> Helpers.addLog UiStrings.statusOrnamentCancelled
            , Cmd.none
            )

        _ ->
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
                        FinishOrnament ->
                            True

                        _ ->
                            False

                -- Check if the raw action was triggered by Enter
                -- We handle this via the OrnamentCancel action check below
                ornamentAction =
                    OrnamentMode.transition model.ornamentMode maybeNoteRef isEnter
            in
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
                |> Helpers.addLog UiStrings.statusOrnamentCollecting
            , Cmd.none
            )

        Cancelled ->
            ( { model | ornamentMode = NoOrnament }
                |> Helpers.addLog UiStrings.statusOrnamentCancelled
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



-- SELECTION / CLIPBOARD


handleSelectRight : Model -> ( Model, Cmd Msg )
handleSelectRight model =
    let
        cur =
            Model.cursor model

        anchor =
            case cur.selectionAnchor of
                Just _ ->
                    cur.selectionAnchor

                Nothing ->
                    Just { cycle = cur.cycle, beat = cur.beat, subdivision = { numerator = 0, denominator = 1 } }

        newCursor =
            { cur | selectionAnchor = anchor }
    in
    ( Helpers.updateCursorInPlace newCursor model
    , ApiCursor.nextBeat model.apiBaseUrl newCursor (Model.currentStartingBeat model) GotCursorResult
    )


handleSelectLeft : Model -> ( Model, Cmd Msg )
handleSelectLeft model =
    let
        cur =
            Model.cursor model

        anchor =
            case cur.selectionAnchor of
                Just _ ->
                    cur.selectionAnchor

                Nothing ->
                    Just { cycle = cur.cycle, beat = cur.beat, subdivision = { numerator = 0, denominator = 1 } }

        newCursor =
            { cur | selectionAnchor = anchor }
    in
    ( Helpers.updateCursorInPlace newCursor model
    , ApiCursor.prevBeat model.apiBaseUrl newCursor (Model.currentStartingBeat model) GotCursorResult
    )


handleCopySelection : Model -> ( Model, Cmd Msg )
handleCopySelection model =
    let
        cur =
            Model.cursor model
    in
    case cur.selectionAnchor of
        Just _ ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.copySelection model.apiBaseUrl comp model.currentSectionIndex cur GotClipboardResult
            )

        Nothing ->
            ( Helpers.addLog UiStrings.statusNoSelectionToCopy model, Cmd.none )


handleCutSelection : Model -> ( Model, Cmd Msg )
handleCutSelection model =
    let
        cur =
            Model.cursor model
    in
    case cur.selectionAnchor of
        Just _ ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True }
            , ApiEditor.cutSelection model.apiBaseUrl comp model.currentSectionIndex cur GotClipboardResult
            )

        Nothing ->
            ( Helpers.addLog UiStrings.statusNoSelectionToCut model, Cmd.none )


handleGotClipboardResult : Result Http.Error (ApiResult ClipboardResult) -> Model -> ( Model, Cmd Msg )
handleGotClipboardResult result model =
    Helpers.handleApiResult result
        (\clipResult ->
            let
                snapshot =
                    { composition = clipResult.composition
                    , cursor = clipResult.cursor
                    , sectionIndex = model.currentSectionIndex
                    }

                newModel =
                    { model
                        | history = UndoHistory.push snapshot model.history
                        , pendingApiCall = False
                    }
                        |> Helpers.addLog clipResult.message
            in
            ( newModel
            , Cmd.batch
                [ Ports.copyToClipboard clipResult.clipboardJson
                , Helpers.requestLayout newModel
                ]
            )
        )
        model


handleClipboardContentReceived : String -> Model -> ( Model, Cmd Msg )
handleClipboardContentReceived jsonString model =
    let
        comp =
            Model.composition model

        cur =
            Model.cursor model
    in
    ( { model | pendingApiCall = True }
    , ApiEditor.pasteClipboard model.apiBaseUrl comp model.currentSectionIndex cur jsonString GotEditorResult
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
                        |> Helpers.addLog UiStrings.statusUndo
            in
            ( newModel, Helpers.requestLayout newModel )

        Nothing ->
            ( Helpers.addLog UiStrings.statusNothingToUndo model, Cmd.none )


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
                        |> Helpers.addLog UiStrings.statusRedo
            in
            ( newModel, Helpers.requestLayout newModel )

        Nothing ->
            ( Helpers.addLog UiStrings.statusNothingToRedo model, Cmd.none )
