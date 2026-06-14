module State.Msg exposing (Msg(..))

import Api.Client exposing (ApiResult)
import Api.Reference exposing (NotationColors, ScriptInfo)
import Api.Section exposing (RemoveSectionResult, ReorderSectionResult)
import Http
import Json.Decode as Decode
import Model.Composition exposing (Composition, SectionType)
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (ClipboardResult, EditorResult, SectionGrid)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal)
import Model.Types exposing (Note, SwarScript, Variant)
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
    | SaveFileAs
    | ExportHtml
    | -- Duplicate-tab dialog resolution (see State.Model.PendingTabOpen)
      DuplicateTabSwitch
    | DuplicateTabOpenWithNewName
    | DuplicateTabCancel
    | -- Unsaved-changes dialog (3-button: Cancel / Discard / Save[As])
      UnsavedChangesCancel
    | UnsavedChangesDiscard
    | UnsavedChangesSave
    | -- Autosave tick (debounced — runs on a Time subscription)
      AutosaveTick Time.Posix
    | -- Toolbar: Edit
      Undo
    | Redo
    | -- Toolbar: Script
      ChangeScript SwarScript
    | -- Toolbar: Theme (light/dark CSS palette toggle)
      ToggleTheme
    | -- Toolbar: Section operations
      SelectSection Int
    | AddSection String SectionType
    | RemoveSection Int
    | RenameSection Int String
    | RequestRenameSection Int String
    | MoveSectionUp Int
    | MoveSectionDown Int
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
    | NewDialogSetGatStartingBeat String
    | NewDialogSetAntaraStartingBeat String
    | NewDialogSetTaanStartingBeat String
    | NewDialogSubmit
    | NewDialogCancel
    | -- Dialogs: Properties
      ShowPropsDialog
    | PropsDialogSetTitle String
    | PropsDialogSetTaal String
    | PropsDialogSetStartingBeat Int String
    | PropsDialogSubmit
    | PropsDialogCancel
    | -- Dialogs: About
      ShowAboutDialog
    | CloseAboutDialog
    | -- Dialogs: Support / donate
      ShowSupportDialog
    | CloseSupportDialog
    | -- Dialogs: Keyboard cheat sheet
      ShowKeyboardCheatSheet
    | CloseKeyboardCheatSheet
    | -- External: User Guide (opens GitHub-hosted markdown in a new tab)
      OpenUserGuide
    | -- Command palette (Cmd+K)
      ShowCommandPalette
    | CloseCommandPalette
    | PaletteQueryChanged String
    | PaletteSelectIndex Int
    | PaletteRunSelected
    | PaletteRunIndex Int
    | -- Dialogs: Bug report
      ShowBugReportDialog
    | BugReportSetDescription String
    | BugReportSetEmail String
    | BugReportSubmit
    | BugReportCancel
    | BugReportResult Bool String
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
    | GotExportHtml (Result Http.Error (ApiResult String))
    | GotSerializedComposition (Result Http.Error (ApiResult String))
    | GotParsedComposition (Result Http.Error (ApiResult Composition))
    | -- Starting beat change
      GotStartingBeatResult (Result Http.Error (ApiResult Composition))
    | -- Taal change (re-maps event positions to fit new matras)
      GotTaalChangeResult (Result Http.Error (ApiResult EditorResult))
    | -- Clipboard operations
      GotClipboardResult (Result Http.Error (ApiResult ClipboardResult))
    | ClipboardContentReceived String
    | -- File port responses
      FileSelected String
    | FileLoaded String
    | -- Swar key timing (for grouping detection)
      GotSwarKeyTime Time.Posix Note Variant String
    | -- Tab management
      SwitchTab String
    | CloseTab String
    | NewTab
    | -- File browser
      ToggleFileBrowser
    | -- Google Drive
      ConnectDrive
    | GotDriveAuthResult Decode.Value
    | GotDriveDirListing Decode.Value
    | GotDriveFileContent Decode.Value
    | GotDriveWriteResult Decode.Value
    | GotDriveError String
    | DriveOpenFolder String
    | DriveOpenFile String String
    | DriveToggleBookmark String
    | DriveRefreshFolder String
    | DriveCreateFile String
    | DriveCreateFolder String
    | DriveRenameItem String String
    | DriveDeleteItem String String
    | -- Config persistence
      SaveConfig
    | GotConfigLoaded String
    | -- Debug bridge (WS only)
      DebugCommandReceived Decode.Value
    | -- Debug bridge async completion callbacks (carry the WS request id so
      -- the response can be correlated). These run separately from the
      -- production Got* handlers so we don't accidentally trigger UI side
      -- effects (file download, dialog dismissal) during parity tests.
      DebugResetReceived String (Result Http.Error (ApiResult Composition))
    | DebugDumpReceived String (Result Http.Error (ApiResult String))
    | DebugExportReceived String (Result Http.Error (ApiResult String))
    | DebugSetTaalReceived String (Result Http.Error (ApiResult EditorResult))
    | DebugStrokeReceived String (Result Http.Error (ApiResult EditorResult))
    | -- No-op
      NoOp
