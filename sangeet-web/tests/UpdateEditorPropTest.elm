module UpdateEditorPropTest exposing (suite)

{-| Plan-19 Tier 4 Phase C — properties for the `State.Update` dispatcher
covering pure-model invariants that no specific example can express:

  - **ChangeScript invariants** — for ANY `SwarScript`, dispatching
    `ChangeScript s` leaves `currentScript = s` and never mutates
    unrelated fields (theme, history depth, edit mode, ornament mode).
    Idempotent under repeated dispatch.
  - **ToggleTheme is its own inverse** — two consecutive `ToggleTheme`
    dispatches restore the original theme (no matter the starting theme).
    Theme is a 2-state enum so this also tests both transitions.
  - **NoOp is a fixed point** — `update NoOp m == (m, Cmd.none)` (we
    can't compare Cmds, so we assert model-equal). Belt-and-braces
    against any wrapper accidentally pushing a snapshot.
  - **AddLog invariants** — log is FIFO-capped at 100; latest entry is
    head of list; adding N messages never exceeds 100 entries.
  - **UpdateCursorInPlace preserves composition** — for ANY cursor,
    updating in place leaves the composition identical (it only
    rewrites the cursor field of the snapshot).

Generators are reused from `Generators.Common`. We don't fabricate any
new fuzzers for `Model` / `Msg` themselves — defaultModel from
`TestHelpers` gives us a fixed baseline, and properties vary only the
message inputs.

-}

import Expect
import Fuzz exposing (Fuzzer)
import Generators.Common as Common
import Model.Types exposing (SwarScript(..))
import State.Model as Model exposing (Theme(..))
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import State.Update exposing (update)
import State.Update.Helpers as Helpers
import Test exposing (Test, describe, fuzz, fuzz2)
import TestHelpers exposing (defaultModel)



-- LOCAL FUZZERS
-- Small enums local to this test (SwarScript, Theme). We deliberately do
-- NOT promote these to Generators.Common — they aren't part of the
-- musical domain model, just UI state. If a second test ever needs the
-- same shape, we promote then.


swarScript : Fuzzer SwarScript
swarScript =
    Fuzz.oneOfValues [ Devanagari, Kannada, Telugu, English ]


theme : Fuzzer Theme
theme =
    Fuzz.oneOfValues [ Light, Dark ]



-- PROPERTIES


suite : Test
suite =
    describe "State.Update — pure-model invariants"
        [ propChangeScriptSets
        , propChangeScriptIdempotent
        , propChangeScriptPreservesUnrelatedFields
        , propToggleThemeIsItsOwnInverse
        , propThemeNameRoundTrip
        , propNoOpIsFixedPoint
        , propAddLogCappedAt100
        , propAddLogLatestAtHead
        , propUpdateCursorInPlacePreservesComposition
        ]


propChangeScriptSets : Test
propChangeScriptSets =
    fuzz swarScript "propChangeScriptSets: update (ChangeScript s) sets currentScript = s" <|
        \s ->
            let
                ( newModel, _ ) =
                    update (ChangeScript s) defaultModel
            in
            newModel.currentScript
                |> Expect.equal s


propChangeScriptIdempotent : Test
propChangeScriptIdempotent =
    fuzz swarScript "propChangeScriptIdempotent: dispatching the same ChangeScript twice leaves currentScript unchanged" <|
        \s ->
            let
                ( m1, _ ) =
                    update (ChangeScript s) defaultModel

                ( m2, _ ) =
                    update (ChangeScript s) m1
            in
            m2.currentScript
                |> Expect.equal m1.currentScript


propChangeScriptPreservesUnrelatedFields : Test
propChangeScriptPreservesUnrelatedFields =
    fuzz swarScript "propChangeScriptPreservesUnrelatedFields: ChangeScript never mutates theme/editMode/ornamentMode/history" <|
        \s ->
            let
                ( newModel, _ ) =
                    update (ChangeScript s) defaultModel
            in
            Expect.all
                [ \m -> m.theme |> Expect.equal defaultModel.theme
                , \m -> m.editMode |> Expect.equal defaultModel.editMode
                , \m -> m.ornamentMode |> Expect.equal defaultModel.ornamentMode

                -- History snapshot count must be identical: ChangeScript
                -- is a UI-only change that does NOT push a snapshot.
                , \m -> UndoHistory.present m.history |> Expect.equal (UndoHistory.present defaultModel.history)
                , \m -> m.currentSectionIndex |> Expect.equal defaultModel.currentSectionIndex
                ]
                newModel


propToggleThemeIsItsOwnInverse : Test
propToggleThemeIsItsOwnInverse =
    fuzz theme "propToggleThemeIsItsOwnInverse: ToggleTheme twice restores the original theme" <|
        \startTheme ->
            let
                start =
                    { defaultModel | theme = startTheme }

                ( m1, _ ) =
                    update ToggleTheme start

                ( m2, _ ) =
                    update ToggleTheme m1
            in
            m2.theme
                |> Expect.equal startTheme


propThemeNameRoundTrip : Test
propThemeNameRoundTrip =
    fuzz theme "propThemeNameRoundTrip: parseTheme (themeName t) == t" <|
        \t ->
            Model.themeName t
                |> Model.parseTheme
                |> Expect.equal t


propNoOpIsFixedPoint : Test
propNoOpIsFixedPoint =
    -- One example is sufficient (NoOp has no input), but we run it as
    -- fuzz over a "perturbation" so any future wrapper that touches
    -- model on NoOp (e.g. an autosave tick wired wrong) shows up
    -- regardless of starting state.
    fuzz swarScript "propNoOpIsFixedPoint: NoOp leaves the model unchanged" <|
        \perturbScript ->
            let
                m =
                    { defaultModel | currentScript = perturbScript }

                ( m1, _ ) =
                    update NoOp m
            in
            m1
                |> Expect.equal m


propAddLogCappedAt100 : Test
propAddLogCappedAt100 =
    fuzz (Fuzz.intRange 0 250) "propAddLogCappedAt100: statusLog length stays ≤ 100 regardless of N" <|
        \n ->
            let
                final =
                    List.range 1 n
                        |> List.foldl (\i acc -> Helpers.addLog ("msg-" ++ String.fromInt i) acc) defaultModel
            in
            List.length final.statusLog
                |> Expect.atMost 100


propAddLogLatestAtHead : Test
propAddLogLatestAtHead =
    fuzz2 Common.shortAsciiString Common.shortAsciiString "propAddLogLatestAtHead: head of statusLog is the most recently added entry" <|
        \first second ->
            let
                m1 =
                    Helpers.addLog first defaultModel

                m2 =
                    Helpers.addLog second m1
            in
            case m2.statusLog of
                head :: _ ->
                    head |> Expect.equal second

                [] ->
                    Expect.fail "statusLog must not be empty after addLog"


propUpdateCursorInPlacePreservesComposition : Test
propUpdateCursorInPlacePreservesComposition =
    -- Vary cursor.cycle to make sure updateCursorInPlace doesn't
    -- accidentally re-derive composition fields from the cursor.
    fuzz (Fuzz.intRange 0 8) "propUpdateCursorInPlacePreservesComposition: updateCursorInPlace leaves composition equal" <|
        \cy ->
            let
                originalCursor =
                    Model.cursor defaultModel

                newCursor =
                    { originalCursor | cycle = cy }

                newModel =
                    Helpers.updateCursorInPlace newCursor defaultModel
            in
            Model.composition newModel
                |> Expect.equal (Model.composition defaultModel)
