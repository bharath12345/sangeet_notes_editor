module TestHelpers exposing
    ( defaultModel
    , modelWithHistory
    , defaultComposition
    , defaultCursor
    , defaultSnapshot
    , makeNoteRef
    , insertEditorResult
    , swarEditModel
    , strokeEditModel
    )

import Api.Client exposing (ApiResult(..))
import Model.Composition exposing (Composition, CompositionType(..), SectionType(..))
import Model.Cursor exposing (CursorModel)
import Model.Layout exposing (EditorResult)
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, VibhagMarker(..))
import Model.Types exposing (Note, Octave(..), Variant(..))
import State.Model as Model exposing (EditMode(..), Model)
import State.UndoHistory as UndoHistory exposing (Snapshot)


defaultModel : Model
defaultModel =
    Model.init "http://test-api"


swarEditModel : Model
swarEditModel =
    { defaultModel | editMode = SwarEdit }


strokeEditModel : Model
strokeEditModel =
    { defaultModel | editMode = StrokeEdit }


defaultComposition : Composition
defaultComposition =
    (Model.composition defaultModel)


defaultCursor : CursorModel
defaultCursor =
    (Model.cursor defaultModel)


defaultSnapshot : Snapshot
defaultSnapshot =
    UndoHistory.present defaultModel.history


modelWithHistory : List Snapshot -> Model
modelWithHistory snapshots =
    case snapshots of
        [] ->
            defaultModel

        first :: rest ->
            let
                history =
                    List.foldl UndoHistory.push (UndoHistory.init first) rest
            in
            { defaultModel | history = history }


makeNoteRef : Note -> Variant -> Octave -> { note : Note, variant : Variant, octave : Octave }
makeNoteRef note variant octave =
    { note = note, variant = variant, octave = octave }


insertEditorResult : String -> Composition -> CursorModel -> EditorResult
insertEditorResult msg comp cur =
    { composition = comp
    , cursor = cur
    , message = msg
    }
