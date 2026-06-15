module State.Update.Dialog exposing
    ( handleBugReportCancel
    , handleBugReportSetDescription
    , handleBugReportSetEmail
    , handleBugReportSubmit
    , handleCloseAboutDialog
    , handleCloseCommandPalette
    , handleCloseKeyboardCheatSheet
    , handleCloseSupportDialog
    , handleNewComposition
    , handleNewDialogCancel
    , handleNewDialogSetAntaraStartingBeat
    , handleNewDialogSetArohan
    , handleNewDialogSetAvrohan
    , handleNewDialogSetGatStartingBeat
    , handleNewDialogSetLaya
    , handleNewDialogSetRaag
    , handleNewDialogSetSamvadi
    , handleNewDialogSetScript
    , handleNewDialogSetShowSahitya
    , handleNewDialogSetShowStrokes
    , handleNewDialogSetTaal
    , handleNewDialogSetTaanCount
    , handleNewDialogSetTaanStartingBeat
    , handleNewDialogSetThaat
    , handleNewDialogSetTitle
    , handleNewDialogSetType
    , handleNewDialogSetVadi
    , handleNewDialogSubmit
    , handleNewGotComposition
    , handleOpenUserGuide
    , handlePaletteQueryChanged
    , handlePaletteRunIndex
    , handlePaletteRunSelected
    , handlePaletteSelectIndex
    , handlePropsDialogCancel
    , handlePropsDialogSetStartingBeat
    , handlePropsDialogSetTaal
    , handlePropsDialogSetTitle
    , handlePropsDialogSubmit
    , handleShowAboutDialog
    , handleShowBugReportDialog
    , handleShowCommandPalette
    , handleShowKeyboardCheatSheet
    , handleShowNewDialog
    , handleShowPropsDialog
    , handleShowSupportDialog
    )

{-| Dialog-management handlers: New Composition dialog (and its API
response wiring), Properties dialog, About, Support, Keyboard Cheat Sheet,
Command Palette, Bug Report. Each is a small open/close/setField/submit
family — the New dialog's submit path is heavier because it also handles
the create-composition POST and the duplicate-tab branch on the response.
-}

import Api.Client exposing (ApiResult)
import Api.Composition as ApiComposition
import Api.Editor as ApiEditor
import Http
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Types
    exposing
        ( Laya(..)
        , Octave(..)
        )
import Ports
import State.AppAction as AppAction
import State.Model as Model
    exposing
        ( EditMode(..)
        , Model
        , OrnamentMode(..)
        , PendingTabSource(..)
        )
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update.Helpers as Helpers
import UiStrings
import Util.TabNameResolver


{-| GitHub-hosted user guide entry point. The directory listing renders the
files in order so users land on a browsable index.
-}
userGuideUrl : String
userGuideUrl =
    "https://github.com/bharath12345/sangeet_notes_editor/tree/main/docs/user-guide"



-- NEW DIALOG


handleShowNewDialog : Model -> ( Model, Cmd Msg )
handleShowNewDialog model =
    ( { model | showNewDialog = True }, Cmd.none )


handleNewDialogSetTitle : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetTitle t model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | title = t } }, Cmd.none )


handleNewDialogSetType : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetType t model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | compositionType = t } }, Cmd.none )


handleNewDialogSetRaag : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetRaag r model =
    let
        form =
            model.newDialogForm

        -- Auto-fill raag details if known raag selected
        maybeRaag =
            Helpers.findByName r model.availableRaags

        updatedForm =
            case maybeRaag of
                Just raag ->
                    { form
                        | raagName = r
                        , thaat = raag.thaat |> Maybe.withDefault ""
                        , arohan = raag.arohana |> Maybe.map (String.join " ") |> Maybe.withDefault ""
                        , avrohan = raag.avarohana |> Maybe.map (String.join " ") |> Maybe.withDefault ""
                        , vadi = raag.vadi |> Maybe.withDefault ""
                        , samvadi = raag.samvadi |> Maybe.withDefault ""
                    }

                Nothing ->
                    { form
                        | raagName = r
                        , thaat = ""
                        , arohan = ""
                        , avrohan = ""
                        , vadi = ""
                        , samvadi = ""
                    }
    in
    ( { model | newDialogForm = updatedForm }, Cmd.none )


handleNewDialogSetTaal : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetTaal t model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | taalName = t } }, Cmd.none )


handleNewDialogSetLaya : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetLaya l model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | layaName = l } }, Cmd.none )


handleNewDialogSetTaanCount : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetTaanCount s model =
    let
        form =
            model.newDialogForm

        count =
            String.toInt s |> Maybe.withDefault 0
    in
    ( { model | newDialogForm = { form | taanCount = count } }, Cmd.none )


handleNewDialogSetShowStrokes : Bool -> Model -> ( Model, Cmd Msg )
handleNewDialogSetShowStrokes b model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | showStrokes = b } }, Cmd.none )


handleNewDialogSetShowSahitya : Bool -> Model -> ( Model, Cmd Msg )
handleNewDialogSetShowSahitya b model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | showSahitya = b } }, Cmd.none )


handleNewDialogSetGatStartingBeat : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetGatStartingBeat s model =
    let
        form =
            model.newDialogForm

        beat =
            String.toInt s |> Maybe.withDefault 1 |> max 1
    in
    ( { model | newDialogForm = { form | gatStartingBeat = beat } }, Cmd.none )


handleNewDialogSetAntaraStartingBeat : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetAntaraStartingBeat s model =
    let
        form =
            model.newDialogForm

        beat =
            String.toInt s |> Maybe.withDefault 1 |> max 1
    in
    ( { model | newDialogForm = { form | antaraStartingBeat = beat } }, Cmd.none )


handleNewDialogSetTaanStartingBeat : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetTaanStartingBeat s model =
    let
        form =
            model.newDialogForm

        beat =
            String.toInt s |> Maybe.withDefault 1 |> max 1
    in
    ( { model | newDialogForm = { form | taanStartingBeat = beat } }, Cmd.none )


handleNewDialogSetThaat : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetThaat t model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | thaat = t } }, Cmd.none )


handleNewDialogSetArohan : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetArohan a model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | arohan = a } }, Cmd.none )


handleNewDialogSetAvrohan : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetAvrohan a model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | avrohan = a } }, Cmd.none )


handleNewDialogSetVadi : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetVadi v model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | vadi = v } }, Cmd.none )


handleNewDialogSetSamvadi : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetSamvadi s model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | samvadi = s } }, Cmd.none )


handleNewDialogSetScript : String -> Model -> ( Model, Cmd Msg )
handleNewDialogSetScript s model =
    let
        form =
            model.newDialogForm
    in
    ( { model | newDialogForm = { form | script = s } }, Cmd.none )


handleNewDialogCancel : Model -> ( Model, Cmd Msg )
handleNewDialogCancel model =
    ( { model | showNewDialog = False }, Cmd.none )


{-| Toolbar "New" button: opens the New Composition dialog. Identical to
ShowNewDialog — both Msgs exist so toolbar code and palette code can fire
either by name.
-}
handleNewComposition : Model -> ( Model, Cmd Msg )
handleNewComposition model =
    ( { model | showNewDialog = True }, Cmd.none )


handleNewDialogSubmit : Model -> ( Model, Cmd Msg )
handleNewDialogSubmit model =
    let
        form =
            model.newDialogForm

        -- Validation
        errors =
            []
                |> (\acc ->
                        if String.trim form.title == "" then
                            "Title is required" :: acc

                        else
                            acc
                   )
                |> (\acc ->
                        if (form.compositionType == "gat" || form.compositionType == "bandish") && form.layaName == "none" then
                            "Laya is required for Gat and Bandish" :: acc

                        else
                            acc
                   )
    in
    if not (List.isEmpty errors) then
        let
            updatedForm =
                { form | validationErrors = List.reverse errors }
        in
        ( { model | newDialogForm = updatedForm }, Cmd.none )

    else
        let
            maybeTaal =
                Helpers.findByName form.taalName model.availableTaals

            maybeRaag =
                Helpers.findByName form.raagName model.availableRaags
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

                            "sargam" ->
                                Sargam

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
                    , gatStartingBeat = form.gatStartingBeat
                    , antaraStartingBeat = form.antaraStartingBeat
                    , taanStartingBeat = form.taanStartingBeat
                    }
                    GotNewComposition
                )

            _ ->
                let
                    updatedForm =
                        { form | validationErrors = [ "Please select valid taal and raag" ] }
                in
                ( { model | newDialogForm = updatedForm }, Cmd.none )


{-| Handle the New Composition API response: opens the freshly-created
composition in a new tab (or surfaces the duplicate-tab dialog if a tab
with the same title is already open).
-}
handleNewGotComposition : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleNewGotComposition result model =
    Helpers.handleApiResult result
        (\comp ->
            let
                savedModel =
                    Model.saveActiveTabState model

                existingTitles =
                    savedModel.tabs |> List.map .filename
            in
            if List.member comp.metadata.title existingTitles then
                let
                    conflicting =
                        savedModel.tabs
                            |> List.filter (\t -> t.filename == comp.metadata.title)
                            |> List.head
                            |> Maybe.map .id
                            |> Maybe.withDefault ""

                    proposed =
                        Util.TabNameResolver.nextAvailableTitle comp.metadata.title existingTitles

                    pending =
                        { composition = comp
                        , source = PendingFromNewComposition
                        , proposedTitle = proposed
                        , conflictingTabId = conflicting
                        }
                in
                ( { savedModel
                    | showNewDialog = False
                    , pendingApiCall = False
                    , pendingTabOpen = Just pending
                    , showDuplicateTabDialog = True
                  }
                , Cmd.none
                )

            else
                let
                    firstStartingBeat =
                        comp.sections
                            |> List.head
                            |> Maybe.map .startingBeat
                            |> Maybe.withDefault 1

                    defaultCursor =
                        { taal = comp.metadata.taal
                        , cycle = 0
                        , beat = firstStartingBeat - 1
                        , subIndex = 0
                        , totalSubdivisions = 1
                        , currentOctave = Madhya
                        , selectionAnchor = Nothing
                        }

                    snapshot =
                        { composition = comp
                        , cursor = defaultCursor
                        , sectionIndex = 0
                        }

                    newHistory =
                        UndoHistory.init snapshot

                    tabId =
                        "tab-" ++ String.fromInt model.nextTabId

                    newTab =
                        { id = tabId
                        , filename = comp.metadata.title
                        , filePath = Nothing
                        , isReadOnly = False
                        , history = newHistory
                        , currentSectionIndex = 0
                        , editMode = SwarEdit
                        , ornamentMode = NoOrnament
                        , groupingState = Nothing
                        , layoutGrids = []
                        , isDirty = False
                        }

                    newModel =
                        { savedModel
                            | history = newHistory
                            , currentSectionIndex = 0
                            , editMode = SwarEdit
                            , ornamentMode = NoOrnament
                            , groupingState = Nothing
                            , layoutGrids = []
                            , showNewDialog = False
                            , pendingApiCall = False
                            , tabs = savedModel.tabs ++ [ newTab ]
                            , activeTabId = Just tabId
                            , nextTabId = model.nextTabId + 1
                        }
                            |> Helpers.addLog (UiStrings.statusCreated |> String.replace "{title}" comp.metadata.title)
                in
                ( newModel, Helpers.requestLayout newModel )
        )
        model



-- PROPERTIES DIALOG


handleShowPropsDialog : Model -> ( Model, Cmd Msg )
handleShowPropsDialog model =
    let
        comp =
            Model.composition model

        isGatOrBandish =
            comp.metadata.compositionType == Gat || comp.metadata.compositionType == Bandish

        sectionBeats =
            if not isGatOrBandish then
                []

            else
                let
                    indexed =
                        List.indexedMap Tuple.pair comp.sections

                    mainEntry =
                        indexed
                            |> List.filter
                                (\( _, s ) ->
                                    s.sectionType == Sthayi || s.sectionType == CustomSectionType "Gat"
                                )
                            |> List.head
                            |> Maybe.map
                                (\( i, s ) ->
                                    let
                                        mainLabel =
                                            if comp.metadata.compositionType == Bandish then
                                                "Sthayi"

                                            else
                                                "Gat"
                                    in
                                    { sectionIndex = i, name = mainLabel, startingBeat = s.startingBeat }
                                )

                    antaraEntry =
                        indexed
                            |> List.filter (\( _, s ) -> s.sectionType == Antara)
                            |> List.head
                            |> Maybe.map
                                (\( i, s ) ->
                                    { sectionIndex = i, name = "Antara", startingBeat = s.startingBeat }
                                )

                    taanEntry =
                        indexed
                            |> List.filter (\( _, s ) -> s.sectionType == Taan)
                            |> List.head
                            |> Maybe.map
                                (\( i, s ) ->
                                    { sectionIndex = i, name = "Taan", startingBeat = s.startingBeat }
                                )
                in
                List.filterMap identity [ mainEntry, antaraEntry, taanEntry ]

        compTypeStr =
            case comp.metadata.compositionType of
                Gat ->
                    "gat"

                Bandish ->
                    "bandish"

                _ ->
                    ""
    in
    ( { model
        | showPropsDialog = True
        , propsDialogForm =
            { title = comp.metadata.title
            , taalName = String.toLower comp.metadata.taal.name
            , sectionStartingBeats = sectionBeats
            , compositionType = compTypeStr
            }
      }
    , Cmd.none
    )


handlePropsDialogSetTitle : String -> Model -> ( Model, Cmd Msg )
handlePropsDialogSetTitle t model =
    let
        form =
            model.propsDialogForm
    in
    ( { model | propsDialogForm = { form | title = t } }, Cmd.none )


handlePropsDialogSetTaal : String -> Model -> ( Model, Cmd Msg )
handlePropsDialogSetTaal t model =
    let
        form =
            model.propsDialogForm
    in
    ( { model | propsDialogForm = { form | taalName = t } }, Cmd.none )


handlePropsDialogSetStartingBeat : Int -> String -> Model -> ( Model, Cmd Msg )
handlePropsDialogSetStartingBeat sectionIndex beatStr model =
    let
        form =
            model.propsDialogForm

        beat =
            String.toInt beatStr |> Maybe.withDefault 1 |> max 1

        updatedBeats =
            List.map
                (\entry ->
                    if entry.sectionIndex == sectionIndex then
                        { entry | startingBeat = beat }

                    else
                        entry
                )
                form.sectionStartingBeats
    in
    ( { model | propsDialogForm = { form | sectionStartingBeats = updatedBeats } }, Cmd.none )


handlePropsDialogSubmit : Model -> ( Model, Cmd Msg )
handlePropsDialogSubmit model =
    let
        form =
            model.propsDialogForm

        maybeTaal =
            Helpers.findByName form.taalName model.availableTaals
    in
    case maybeTaal of
        Just newTaal ->
            let
                comp =
                    Model.composition model

                meta =
                    comp.metadata

                -- Always apply title change locally; the taal change
                -- goes through the server below if needed.
                compWithTitle =
                    { comp | metadata = { meta | title = form.title } }

                taalChanged =
                    comp.metadata.taal.name /= newTaal.name

                changedBeats =
                    form.sectionStartingBeats
                        |> List.filterMap
                            (\entry ->
                                let
                                    currentBeat =
                                        compWithTitle.sections
                                            |> List.drop entry.sectionIndex
                                            |> List.head
                                            |> Maybe.map .startingBeat
                                            |> Maybe.withDefault 1
                                in
                                if entry.startingBeat /= currentBeat then
                                    Just ( entry.sectionIndex, entry.startingBeat )

                                else
                                    Nothing
                            )
            in
            if taalChanged then
                -- Server endpoint re-maps event positions so events
                -- past the new taal's matras flow into subsequent
                -- cycles. The response carries the re-mapped
                -- composition + a fresh cursor; the result handler
                -- pushes the snapshot and continues with any
                -- pending starting-beat changes.
                ( { model
                    | showPropsDialog = False
                    , pendingApiCall = True
                    , pendingStartingBeatChanges = changedBeats
                  }
                    |> Helpers.addLog (UiStrings.statusPropertiesUpdatedTaal |> String.replace "{taalName}" newTaal.name)
                , ApiEditor.changeTaal
                    model.apiBaseUrl
                    compWithTitle
                    model.currentSectionIndex
                    newTaal
                    GotTaalChangeResult
                )

            else
                -- No taal change: keep the original local-snapshot
                -- path. Title still flows through `compWithTitle`.
                let
                    cur =
                        Model.cursor model

                    newSectionStartBeat =
                        let
                            formBeat =
                                form.sectionStartingBeats
                                    |> List.filter (\e -> e.sectionIndex == model.currentSectionIndex)
                                    |> List.head
                                    |> Maybe.map .startingBeat
                        in
                        case formBeat of
                            Just b ->
                                b

                            Nothing ->
                                compWithTitle.sections
                                    |> List.drop model.currentSectionIndex
                                    |> List.head
                                    |> Maybe.map .startingBeat
                                    |> Maybe.withDefault 1

                    newCursor =
                        { cur | taal = newTaal, cycle = 0, beat = newSectionStartBeat - 1, subIndex = 0, totalSubdivisions = 1 }

                    snapshot =
                        { composition = compWithTitle
                        , cursor = newCursor
                        , sectionIndex = model.currentSectionIndex
                        }

                    baseModel =
                        { model
                            | history = UndoHistory.push snapshot model.history
                            , showPropsDialog = False
                        }
                            |> Helpers.addLog (UiStrings.statusPropertiesUpdatedTaal |> String.replace "{taalName}" newTaal.name)
                in
                case changedBeats of
                    ( sectionIdx, beatVal ) :: rest ->
                        ( { baseModel
                            | pendingStartingBeatChanges = rest
                            , pendingApiCall = True
                          }
                        , ApiEditor.changeStartingBeat
                            model.apiBaseUrl
                            compWithTitle
                            sectionIdx
                            beatVal
                            GotStartingBeatResult
                        )

                    [] ->
                        ( baseModel, Helpers.requestLayout baseModel )

        Nothing ->
            ( { model | showPropsDialog = False }
                |> Helpers.addLog UiStrings.statusPropertiesUpdatedTaalNotFound
            , Cmd.none
            )


handlePropsDialogCancel : Model -> ( Model, Cmd Msg )
handlePropsDialogCancel model =
    ( { model | showPropsDialog = False }, Cmd.none )



-- ABOUT / SUPPORT / KEYBOARD CHEAT SHEET


handleShowAboutDialog : Model -> ( Model, Cmd Msg )
handleShowAboutDialog model =
    ( { model | showAboutDialog = True }, Cmd.none )


handleCloseAboutDialog : Model -> ( Model, Cmd Msg )
handleCloseAboutDialog model =
    ( { model | showAboutDialog = False }, Cmd.none )


handleShowSupportDialog : Model -> ( Model, Cmd Msg )
handleShowSupportDialog model =
    ( { model | showSupportDialog = True }, Cmd.none )


handleCloseSupportDialog : Model -> ( Model, Cmd Msg )
handleCloseSupportDialog model =
    ( { model | showSupportDialog = False }, Cmd.none )


handleShowKeyboardCheatSheet : Model -> ( Model, Cmd Msg )
handleShowKeyboardCheatSheet model =
    ( { model | showKeyboardCheatSheet = True }, Cmd.none )


handleCloseKeyboardCheatSheet : Model -> ( Model, Cmd Msg )
handleCloseKeyboardCheatSheet model =
    ( { model | showKeyboardCheatSheet = False }, Cmd.none )


handleOpenUserGuide : Model -> ( Model, Cmd Msg )
handleOpenUserGuide model =
    ( model, Ports.openExternalUrl userGuideUrl )



-- COMMAND PALETTE


handleShowCommandPalette : Model -> ( Model, Cmd Msg )
handleShowCommandPalette model =
    ( { model | showCommandPalette = True, paletteQuery = "", paletteSelectedIndex = 0 }, Cmd.none )


handleCloseCommandPalette : Model -> ( Model, Cmd Msg )
handleCloseCommandPalette model =
    ( { model | showCommandPalette = False }, Cmd.none )


handlePaletteQueryChanged : String -> Model -> ( Model, Cmd Msg )
handlePaletteQueryChanged q model =
    ( { model | paletteQuery = q, paletteSelectedIndex = 0 }, Cmd.none )


handlePaletteSelectIndex : Int -> Model -> ( Model, Cmd Msg )
handlePaletteSelectIndex i model =
    let
        results =
            AppAction.filter model.paletteQuery (AppAction.all model.currentSectionIndex)

        clamped =
            max 0 (min i (List.length results - 1))
    in
    ( { model | paletteSelectedIndex = clamped }, Cmd.none )


{-| Look up the AppAction at the given filtered-list index and dispatch its Msg by
recursively calling update. Closes the palette regardless of whether the index was
valid (no-op if the index falls outside the filtered list).
-}
runPaletteAction : (Msg -> Model -> ( Model, Cmd Msg )) -> Int -> Model -> ( Model, Cmd Msg )
runPaletteAction runUpdate i model =
    let
        results =
            AppAction.filter model.paletteQuery (AppAction.all model.currentSectionIndex)

        closed =
            { model | showCommandPalette = False }
    in
    case List.head (List.drop i results) of
        Just action ->
            runUpdate action.msg closed

        Nothing ->
            ( closed, Cmd.none )


handlePaletteRunSelected : (Msg -> Model -> ( Model, Cmd Msg )) -> Model -> ( Model, Cmd Msg )
handlePaletteRunSelected runUpdate model =
    runPaletteAction runUpdate model.paletteSelectedIndex model


handlePaletteRunIndex : (Msg -> Model -> ( Model, Cmd Msg )) -> Int -> Model -> ( Model, Cmd Msg )
handlePaletteRunIndex runUpdate i model =
    runPaletteAction runUpdate i model



-- BUG REPORT DIALOG


handleShowBugReportDialog : Model -> ( Model, Cmd Msg )
handleShowBugReportDialog model =
    ( { model
        | showBugReportDialog = True
        , bugReportForm = Model.defaultBugReportForm
      }
    , Cmd.none
    )


handleBugReportSetDescription : String -> Model -> ( Model, Cmd Msg )
handleBugReportSetDescription d model =
    let
        form =
            model.bugReportForm
    in
    ( { model | bugReportForm = { form | description = d } }, Cmd.none )


handleBugReportSetEmail : String -> Model -> ( Model, Cmd Msg )
handleBugReportSetEmail e model =
    let
        form =
            model.bugReportForm
    in
    ( { model | bugReportForm = { form | email = e } }, Cmd.none )


handleBugReportSubmit : Model -> ( Model, Cmd Msg )
handleBugReportSubmit model =
    let
        form =
            model.bugReportForm
    in
    ( { model | bugReportForm = { form | sending = True } }
    , Ports.submitBugReport
        { description = String.trim form.description
        , email = String.trim form.email
        , apiBaseUrl = model.apiBaseUrl
        }
    )


handleBugReportCancel : Model -> ( Model, Cmd Msg )
handleBugReportCancel model =
    ( { model
        | showBugReportDialog = False
        , bugReportForm = Model.defaultBugReportForm
      }
    , Cmd.none
    )
