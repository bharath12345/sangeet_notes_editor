module UndoHistoryTest exposing (countUndos, defaultRaag, defaultTaal, initTests, makeSnapshot, predicateTests, pushTests, redoTests, snap0, snap1, snap2, suite, trimTests, undoTests)

import Expect
import Model.Composition exposing (CompositionType(..), SectionType(..))
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, VibhagMarker(..))
import Model.Types exposing (Octave(..))
import State.UndoHistory as UndoHistory exposing (Snapshot, UndoHistory)
import Test exposing (Test, describe, test)


defaultTaal : Taal
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


defaultRaag : Raag
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


makeSnapshot : String -> Int -> Snapshot
makeSnapshot title idx =
    { composition =
        { metadata =
            { title = title
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
    , cursor =
        { taal = defaultTaal
        , cycle = 0
        , beat = 0
        , subIndex = 0
        , totalSubdivisions = 1
        , currentOctave = Madhya
        , selectionAnchor = Nothing
        }
    , sectionIndex = idx
    }


snap0 : Snapshot
snap0 =
    makeSnapshot "Initial" 0


snap1 : Snapshot
snap1 =
    makeSnapshot "After edit 1" 0


snap2 : Snapshot
snap2 =
    makeSnapshot "After edit 2" 0


suite : Test
suite =
    describe "UndoHistory"
        [ initTests
        , pushTests
        , undoTests
        , redoTests
        , predicateTests
        , trimTests
        ]


initTests : Test
initTests =
    describe "init"
        [ test "present returns initial snapshot" <|
            \_ ->
                let
                    history =
                        UndoHistory.init snap0
                in
                (UndoHistory.present history).composition.metadata.title
                    |> Expect.equal "Initial"
        , test "canUndo is False after init" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.canUndo
                    |> Expect.equal False
        , test "canRedo is False after init" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.canRedo
                    |> Expect.equal False
        ]


pushTests : Test
pushTests =
    describe "push"
        [ test "push updates present" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.present
                    |> .composition
                    |> .metadata
                    |> .title
                    |> Expect.equal "After edit 1"
        , test "push enables canUndo" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.canUndo
                    |> Expect.equal True
        , test "push clears redo stack" <|
            \_ ->
                let
                    history =
                        UndoHistory.init snap0
                            |> UndoHistory.push snap1
                            |> UndoHistory.push snap2

                    afterUndo =
                        UndoHistory.undo history

                    afterPush =
                        afterUndo
                            |> Maybe.map (UndoHistory.push (makeSnapshot "New" 0))
                in
                afterPush
                    |> Maybe.map UndoHistory.canRedo
                    |> Expect.equal (Just False)
        ]


undoTests : Test
undoTests =
    describe "undo"
        [ test "undo on empty past returns Nothing" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.undo
                    |> Expect.equal Nothing
        , test "undo restores previous snapshot" <|
            \_ ->
                let
                    result =
                        UndoHistory.init snap0
                            |> UndoHistory.push snap1
                            |> UndoHistory.undo
                            |> Maybe.map UndoHistory.present
                            |> Maybe.map (.composition >> .metadata >> .title)
                in
                result |> Expect.equal (Just "Initial")
        , test "undo then redo restores original" <|
            \_ ->
                let
                    result =
                        UndoHistory.init snap0
                            |> UndoHistory.push snap1
                            |> UndoHistory.undo
                            |> Maybe.andThen UndoHistory.redo
                            |> Maybe.map UndoHistory.present
                            |> Maybe.map (.composition >> .metadata >> .title)
                in
                result |> Expect.equal (Just "After edit 1")
        ]


redoTests : Test
redoTests =
    describe "redo"
        [ test "redo on empty future returns Nothing" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.redo
                    |> Expect.equal Nothing
        , test "redo after undo returns next snapshot" <|
            \_ ->
                let
                    result =
                        UndoHistory.init snap0
                            |> UndoHistory.push snap1
                            |> UndoHistory.push snap2
                            |> UndoHistory.undo
                            |> Maybe.andThen UndoHistory.redo
                            |> Maybe.map UndoHistory.present
                            |> Maybe.map (.composition >> .metadata >> .title)
                in
                result |> Expect.equal (Just "After edit 2")
        ]


predicateTests : Test
predicateTests =
    describe "canUndo / canRedo predicates"
        [ test "canUndo True after push" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.canUndo
                    |> Expect.equal True
        , test "canUndo False after undoing all" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.undo
                    |> Maybe.map UndoHistory.canUndo
                    |> Expect.equal (Just False)
        , test "canRedo True after undo" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.undo
                    |> Maybe.map UndoHistory.canRedo
                    |> Expect.equal (Just True)
        , test "canRedo False with no future" <|
            \_ ->
                UndoHistory.init snap0
                    |> UndoHistory.push snap1
                    |> UndoHistory.canRedo
                    |> Expect.equal False
        ]


trimTests : Test
trimTests =
    describe "maxHistory trimming"
        [ test "past is trimmed to 50 entries" <|
            \_ ->
                let
                    history =
                        List.range 1 55
                            |> List.foldl
                                (\i h -> UndoHistory.push (makeSnapshot ("Edit " ++ String.fromInt i) 0) h)
                                (UndoHistory.init snap0)

                    undoCount =
                        countUndos history 0
                in
                undoCount |> Expect.equal 50
        ]


countUndos : UndoHistory -> Int -> Int
countUndos history count =
    case UndoHistory.undo history of
        Nothing ->
            count

        Just h ->
            countUndos h (count + 1)
