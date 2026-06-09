module State.Model exposing
    ( DriveItem
    , DriveState(..)
    , EditMode(..)
    , FileTab
    , FolderState
    , GroupingState
    , Model
    , NewDialogForm
    , OrnamentMode(..)
    , PropsDialogForm
    , activeTab
    , composition
    , cursor
    , defaultLayoutConfig
    , init
    , loadTabState
    , saveActiveTabState
    , sectionIndex
    )

import Api.Reference exposing (NotationColors, ScriptInfo)
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
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
    }



-- PROPS DIALOG FORM


type alias PropsDialogForm =
    { title : String
    , taalName : String
    }



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
    , cursorVisible : Bool
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
    , showKeyboardLegend : Bool
    , pendingApiCall : Bool
    , tabs : List FileTab
    , activeTabId : Maybe String
    , nextTabId : Int
    , driveState : DriveState
    , driveFolders : List FolderState
    , fileBrowserCollapsed : Bool
    , fileBrowserWidth : Float
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


init : String -> Model
init apiBaseUrl =
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
            }
    in
    { apiBaseUrl = apiBaseUrl
    , history = initialHistory
    , currentSectionIndex = 0
    , editMode = SwarEdit
    , ornamentMode = NoOrnament
    , currentScript = Devanagari
    , groupingState = Nothing
    , cursorVisible = True
    , statusLog = [ "Welcome to Sangeet Notes Editor" ]
    , availableTaals = []
    , availableRaags = []
    , notationColors = Nothing
    , availableScripts = []
    , layoutGrids = []
    , showNewDialog = False
    , newDialogForm = defaultNewDialogForm
    , showPropsDialog = False
    , propsDialogForm = { title = "", taalName = "" }
    , showAboutDialog = False
    , showKeyboardLegend = False
    , pendingApiCall = False
    , tabs = [ initialTab ]
    , activeTabId = Just "tab-1"
    , nextTabId = 2
    , driveState = DriveDisconnected
    , driveFolders = []
    , fileBrowserCollapsed = True
    , fileBrowserWidth = 250.0
    }



-- ACCESSORS (convenience functions for reading from undo history)


composition : Model -> Composition
composition model =
    (UndoHistory.present model.history).composition


cursor : Model -> CursorModel
cursor model =
    (UndoHistory.present model.history).cursor


sectionIndex : Model -> Int
sectionIndex model =
    (UndoHistory.present model.history).sectionIndex



-- TAB HELPERS


activeTab : Model -> Maybe FileTab
activeTab model =
    model.activeTabId
        |> Maybe.andThen
            (\id ->
                model.tabs
                    |> List.filter (\t -> t.id == id)
                    |> List.head
            )


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
