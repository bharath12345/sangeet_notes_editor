module State.Msg exposing (Msg(..))

import Api.Client exposing (ApiResult)
import Api.Reference exposing (NotationColors, ScriptInfo)
import Api.Section exposing (RemoveSectionResult, ReorderSectionResult)
import Http
import Json.Decode as Decode
import Model.Composition exposing (Composition, SectionType)
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (EditorResult, SectionGrid, TimedNote)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal)
import Model.Types exposing (Laya, Note, Octave, SwarScript, Variant)
import Time


type Msg
    = -- Keyboard
      KeyPressed String Bool Bool Bool
    | -- Mouse
      CanvasClicked Int Int
    | -- Toolbar: File operations
      NewComposition
    | OpenFile
    | SaveFile
    | ExportPdf
    | ExportHtml
    | -- Toolbar: Edit
      Undo
    | Redo
    | -- Toolbar: Script
      ChangeScript SwarScript
    | -- Toolbar: Section operations
      SelectSection Int
    | AddSection String SectionType
    | RemoveSection Int
    | RenameSection Int String
    | MoveSectionUp Int
    | MoveSectionDown Int
    | -- Toolbar: Playback
      Play
    | Pause
    | Stop
    | SetBpm Float
    | ToggleLoop
    | -- Toolbar: View toggles
      ToggleStrokeLine
    | ToggleSahityaLine
    | ToggleKeyboardLegend
    | -- Dialogs: New Composition
      ShowNewDialog
    | NewDialogSetTitle String
    | NewDialogSetType String
    | NewDialogSetRaag String
    | NewDialogSetTaal String
    | NewDialogSetLaya String
    | NewDialogSetTaanCount String
    | NewDialogSetShowStrokes Bool
    | NewDialogSetShowSahitya Bool
    | NewDialogSubmit
    | NewDialogCancel
    | -- Dialogs: Properties
      ShowPropsDialog
    | PropsDialogSetTitle String
    | PropsDialogSetTaal String
    | PropsDialogSubmit
    | PropsDialogCancel
    | -- Dialogs: About
      ShowAboutDialog
    | CloseAboutDialog
    | -- API responses
      GotEditorResult (Result Http.Error (ApiResult EditorResult))
    | GotCursorResult (Result Http.Error (ApiResult CursorModel))
    | GotLayoutResult (Result Http.Error (ApiResult (List SectionGrid)))
    | GotTaals (Result Http.Error (ApiResult (List ( String, Taal ))))
    | GotRaags (Result Http.Error (ApiResult (List ( String, Raag ))))
    | GotColors (Result Http.Error (ApiResult NotationColors))
    | GotScripts (Result Http.Error (ApiResult (List ( String, ScriptInfo ))))
    | GotNewComposition (Result Http.Error (ApiResult Composition))
    | GotSectionAdd (Result Http.Error (ApiResult Composition))
    | GotSectionRemove (Result Http.Error (ApiResult RemoveSectionResult))
    | GotSectionRename (Result Http.Error (ApiResult Composition))
    | GotSectionReorder (Result Http.Error (ApiResult ReorderSectionResult))
    | GotPlaybackSchedule (Result Http.Error (ApiResult (List TimedNote)))
    | GotExportHtml (Result Http.Error (ApiResult String))
    | GotSerializedComposition (Result Http.Error (ApiResult Decode.Value))
    | GotParsedComposition (Result Http.Error (ApiResult Composition))
    | -- File port responses
      FileSelected String
    | FileLoaded String
    | -- Swar key timing (for grouping detection)
      GotSwarKeyTime Time.Posix Note Variant String
    | -- Timers
      CursorBlink Time.Posix
    | -- No-op
      NoOp
