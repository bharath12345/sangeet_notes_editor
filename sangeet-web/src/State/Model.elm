module State.Model exposing
    ( BugReportForm
    , DriveItem
    , DriveState(..)
    , EditMode(..)
    , FileTab
    , FolderState
    , GroupingState
    , Model
    , NewDialogForm
    , OrnamentMode(..)
    , PendingTabOpen
    , PendingTabSource(..)
    , PropsDialogForm
    , SectionStartingBeatEntry
    , Theme(..)
    , composition
    , currentSectionMaxCycle
    , currentStartingBeat
    , cursor
    , defaultBugReportForm
    , defaultLayoutConfig
    , init
    , loadTabState
    , parseTheme
    , saveActiveTabState
    , themeName
    )

import Api.Reference exposing (NotationColors, ScriptInfo)
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Event exposing (Event(..))
import Model.Layout exposing (LayoutConfig, SectionGrid)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, VibhagMarker(..))
import Model.Types
    exposing
        ( MeendDirection
        , Note
        , NoteRef
        , Octave(..)
        , SwarScript(..)
        , Variant
        )
import State.UndoHistory as UndoHistory exposing (UndoHistory)



-- THEME
-- Mirrors desktop's ThemeManager.Theme. The selected theme persists to
-- localStorage via a port (init reads it back via a flag from index.html
-- so the right palette renders on first paint, avoiding a Light→Dark
-- flash on reload).


type Theme
    = Light
    | Dark


themeName : Theme -> String
themeName t =
    case t of
        Light ->
            "light"

        Dark ->
            "dark"


parseTheme : String -> Theme
parseTheme s =
    case String.toLower s of
        "dark" ->
            Dark

        _ ->
            Light



-- EDIT MODE


type EditMode
    = SwarEdit
    | StrokeEdit



-- ORNAMENT MODE


type OrnamentMode
    = NoOrnament
    | SingleNoteMode String
    | MeendStartMode MeendDirection
    | MeendEndMode NoteRef MeendDirection
    | KrintanStartMode
    | KrintanEndMode NoteRef
    | MurkiCollectMode (List NoteRef)
    | ZamzamaCollectMode (List NoteRef)



-- NEW DIALOG FORM


type alias NewDialogForm =
    { title : String
    , compositionType : String
    , raagName : String
    , taalName : String
    , layaName : String
    , taanCount : Int
    , showStrokes : Bool
    , showSahitya : Bool
    , gatStartingBeat : Int
    , antaraStartingBeat : Int
    , taanStartingBeat : Int
    }


defaultNewDialogForm : NewDialogForm
defaultNewDialogForm =
    { title = ""
    , compositionType = "gat"
    , raagName = "yaman"
    , taalName = "teentaal"
    , layaName = "vilambit"
    , taanCount = 0
    , showStrokes = True
    , showSahitya = False
    , gatStartingBeat = 1
    , antaraStartingBeat = 1
    , taanStartingBeat = 1
    }



-- PROPS DIALOG FORM


type alias SectionStartingBeatEntry =
    { sectionIndex : Int
    , name : String
    , startingBeat : Int
    }


type alias PropsDialogForm =
    { title : String
    , taalName : String
    , sectionStartingBeats : List SectionStartingBeatEntry
    , compositionType : String
    }



-- BUG REPORT DIALOG FORM


type alias BugReportForm =
    { description : String
    , email : String
    , sending : Bool
    }


defaultBugReportForm : BugReportForm
defaultBugReportForm =
    { description = "", email = "", sending = False }



-- GROUPING STATE


type alias GroupingState =
    { notes : List { note : Note, variant : Variant, octave : Octave }
    , startTime : Int
    , beat : Int
    , cycle : Int
    }



-- FILE TAB


type alias FileTab =
    { id : String
    , filename : String
    , filePath : Maybe String
    , isReadOnly : Bool
    , history : UndoHistory
    , currentSectionIndex : Int
    , editMode : EditMode
    , ornamentMode : OrnamentMode
    , groupingState : Maybe GroupingState
    , layoutGrids : List SectionGrid
    , isDirty : Bool
    }



-- PENDING TAB OPEN (duplicate-tab confirmation)
-- When opening a file or creating a new composition would produce a tab whose
-- display name matches an already-open tab, we stash the parsed composition
-- here while the duplicate-tab modal is up. The user's choice (switch /
-- rename / cancel) consumes this slot.


type PendingTabSource
    = PendingFromNewComposition
    | PendingFromOpenedFile


type alias PendingTabOpen =
    { composition : Composition
    , source : PendingTabSource
    , proposedTitle : String
    , conflictingTabId : String
    }



-- GOOGLE DRIVE STATE


type DriveState
    = DriveDisconnected
    | DriveConnecting
    | DriveConnected


type alias DriveItem =
    { id : String
    , name : String
    , mimeType : String
    , isBookmarked : Bool
    }


type alias FolderState =
    { folderId : String
    , name : String
    , items : List DriveItem
    , expanded : Bool
    , isBookmarked : Bool
    }



-- MODEL


type alias Model =
    { apiBaseUrl : String
    , history : UndoHistory
    , currentSectionIndex : Int
    , editMode : EditMode
    , ornamentMode : OrnamentMode
    , currentScript : SwarScript
    , groupingState : Maybe GroupingState
    , statusLog : List String
    , availableTaals : List ( String, Taal )
    , availableRaags : List ( String, Raag )
    , notationColors : Maybe NotationColors
    , availableScripts : List ( String, ScriptInfo )
    , layoutGrids : List SectionGrid
    , showNewDialog : Bool
    , newDialogForm : NewDialogForm
    , showPropsDialog : Bool
    , propsDialogForm : PropsDialogForm
    , showAboutDialog : Bool
    , showSupportDialog : Bool
    , showBugReportDialog : Bool
    , bugReportForm : BugReportForm
    , showKeyboardCheatSheet : Bool
    , showCommandPalette : Bool
    , paletteQuery : String
    , paletteSelectedIndex : Int
    , pendingApiCall : Bool
    , pendingStartingBeatChanges : List ( Int, Int )
    , tabs : List FileTab
    , activeTabId : Maybe String
    , nextTabId : Int
    , driveState : DriveState
    , driveFolders : List FolderState
    , fileBrowserCollapsed : Bool
    , fileBrowserWidth : Float
    , pendingDebugAck : Maybe String
    , pendingTabOpen : Maybe PendingTabOpen
    , showDuplicateTabDialog : Bool
    , showUnsavedChangesDialog : Maybe String
    , pendingSaveAs : Bool
    , theme : Theme
    }



-- INIT


defaultLayoutConfig : LayoutConfig
defaultLayoutConfig =
    { highDensityThreshold = 4
    , cellWidthBase = 60.0
    , cellOverflowExpand = 1.5
    , lineSpacing = 20.0
    , headerHeight = 40.0
    }


init : String -> Theme -> Model
init apiBaseUrl initialTheme =
    let
        defaultTaal =
            { name = "Teentaal"
            , matras = 16
            , vibhags =
                [ { beats = 4, marker = Sam }
                , { beats = 4, marker = TaaliMarker 2 }
                , { beats = 4, marker = KhaliMarker }
                , { beats = 4, marker = TaaliMarker 3 }
                ]
            , theka = Nothing
            }

        defaultRaag =
            { name = "Yaman"
            , thaat = Just "Kalyan"
            , arohana = Nothing
            , avarohana = Nothing
            , vadi = Nothing
            , samvadi = Nothing
            , pakad = Nothing
            , prahar = Nothing
            }

        defaultComposition =
            { metadata =
                { title = "Untitled"
                , compositionType = Gat
                , raag = defaultRaag
                , taal = defaultTaal
                , laya = Nothing
                , instrument = Nothing
                , composer = Nothing
                , author = Nothing
                , source = Nothing
                , showStrokeLine = True
                , showSahityaLine = False
                , createdAt = ""
                , updatedAt = ""
                }
            , sections =
                [ { name = "Sthayi"
                  , sectionType = Sthayi
                  , events = []
                  , tihai = Nothing
                  , startingBeat = 1
                  }
                ]
            }

        defaultCursor =
            { taal = defaultTaal
            , cycle = 0
            , beat = 0
            , subIndex = 0
            , totalSubdivisions = 1
            , currentOctave = Madhya
            , selectionAnchor = Nothing
            }

        snapshot =
            { composition = defaultComposition
            , cursor = defaultCursor
            , sectionIndex = 0
            }

        initialHistory =
            UndoHistory.init snapshot

        initialTab =
            { id = "tab-1"
            , filename = "Untitled"
            , filePath = Nothing
            , isReadOnly = False
            , history = initialHistory
            , currentSectionIndex = 0
            , editMode = SwarEdit
            , ornamentMode = NoOrnament
            , groupingState = Nothing
            , layoutGrids = []
            , isDirty = False
            }
    in
    { apiBaseUrl = apiBaseUrl
    , history = initialHistory
    , currentSectionIndex = 0
    , editMode = SwarEdit
    , ornamentMode = NoOrnament
    , currentScript = Devanagari
    , groupingState = Nothing
    , statusLog = [ "Welcome to Sangeet Notes Editor" ]
    , availableTaals = []
    , availableRaags = []
    , notationColors = Nothing
    , availableScripts = []
    , layoutGrids = []
    , showNewDialog = False
    , newDialogForm = defaultNewDialogForm
    , showPropsDialog = False
    , propsDialogForm = { title = "", taalName = "", sectionStartingBeats = [], compositionType = "" }
    , showAboutDialog = False
    , showSupportDialog = False
    , showBugReportDialog = False
    , bugReportForm = defaultBugReportForm
    , showKeyboardCheatSheet = False
    , showCommandPalette = False
    , paletteQuery = ""
    , paletteSelectedIndex = 0
    , pendingApiCall = False
    , pendingStartingBeatChanges = []
    , tabs = [ initialTab ]
    , activeTabId = Just "tab-1"
    , nextTabId = 2
    , driveState = DriveDisconnected
    , driveFolders = []
    , fileBrowserCollapsed = True
    , fileBrowserWidth = 250.0
    , pendingDebugAck = Nothing
    , pendingTabOpen = Nothing
    , showDuplicateTabDialog = False
    , showUnsavedChangesDialog = Nothing
    , pendingSaveAs = False
    , theme = initialTheme
    }



-- ACCESSORS (convenience functions for reading from undo history)


composition : Model -> Composition
composition model =
    (UndoHistory.present model.history).composition


cursor : Model -> CursorModel
cursor model =
    (UndoHistory.present model.history).cursor


currentStartingBeat : Model -> Int
currentStartingBeat model =
    let
        comp =
            composition model
    in
    comp.sections
        |> List.drop model.currentSectionIndex
        |> List.head
        |> Maybe.map .startingBeat
        |> Maybe.withDefault 1


{-| Max cycle index reachable by the cursor in the active section. Mirrors
desktop CompositionEditor.maxCycle: the highest cycle present in the
section's events, or 0 if empty. The cursor is allowed to land on
maxCycle + 1 (the next, currently-empty cycle) — beyond that there is
no rendered cell so the cursor would visually disappear.
-}
currentSectionMaxCycle : Model -> Int
currentSectionMaxCycle model =
    let
        section =
            (composition model).sections
                |> List.drop model.currentSectionIndex
                |> List.head
    in
    case section of
        Nothing ->
            0

        Just s ->
            s.events
                |> List.map eventCycle
                |> List.maximum
                |> Maybe.withDefault 0


eventCycle : Event -> Int
eventCycle event =
    case event of
        SwarEvent r ->
            r.beat.cycle

        RestEvent r ->
            r.beat.cycle

        SustainEvent r ->
            r.beat.cycle

        ChikariEvent r ->
            r.beat.cycle

        LockedBeatEvent r ->
            r.beat.cycle


saveActiveTabState : Model -> Model
saveActiveTabState model =
    case model.activeTabId of
        Just id ->
            { model
                | tabs =
                    List.map
                        (\t ->
                            if t.id == id then
                                { t
                                    | history = model.history
                                    , currentSectionIndex = model.currentSectionIndex
                                    , editMode = model.editMode
                                    , ornamentMode = model.ornamentMode
                                    , groupingState = model.groupingState
                                    , layoutGrids = model.layoutGrids
                                }

                            else
                                t
                        )
                        model.tabs
            }

        Nothing ->
            model


loadTabState : FileTab -> Model -> Model
loadTabState tab model =
    { model
        | history = tab.history
        , currentSectionIndex = tab.currentSectionIndex
        , editMode = tab.editMode
        , ornamentMode = tab.ornamentMode
        , groupingState = tab.groupingState
        , layoutGrids = tab.layoutGrids
        , activeTabId = Just tab.id
    }
