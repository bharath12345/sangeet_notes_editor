module State.Model exposing
    ( EditMode(..)
    , GroupingState
    , Model
    , NewDialogForm
    , OrnamentMode(..)
    , PropsDialogForm
    , composition
    , cursor
    , defaultLayoutConfig
    , init
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


{-| Create the initial empty composition and cursor to bootstrap the app.
Uses Yaman raag with Teentaal as defaults.
-}
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
            }

        snapshot =
            { composition = defaultComposition
            , cursor = defaultCursor
            , sectionIndex = 0
            }
    in
    { apiBaseUrl = apiBaseUrl
    , history = UndoHistory.init snapshot
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
