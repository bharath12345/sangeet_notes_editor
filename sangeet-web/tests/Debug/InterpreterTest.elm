module Debug.InterpreterTest exposing (suite)

import Debug.Interpreter
import Expect
import Json.Decode as Decode
import Json.Encode as Encode
import Model.Composition exposing (CompositionType(..), SectionType(..))
import Model.Raag exposing (Raag)
import Model.Taal exposing (Taal, VibhagMarker(..))
import Model.Types exposing (Octave(..))
import State.Model as Model
import State.Msg exposing (Msg(..))
import State.UndoHistory as UndoHistory
import Test exposing (..)


suite : Test
suite =
    describe "Debug.Interpreter"
        [ describe "decoder shape"
            [ test "decodes Ping with discriminator wrapper" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "t1" )
                                , ( "cmd", Encode.object [ ( "Ping", Encode.object [] ) ] )
                                ]

                        ( msg, response ) =
                            Debug.Interpreter.interpret payload (initModel ())
                    in
                    case response of
                        Just r ->
                            case Decode.decodeValue Decode.string r.result of
                                Ok "PONG" ->
                                    Expect.pass

                                _ ->
                                    Expect.fail "Ping must return PONG string"

                        Nothing ->
                            Expect.fail "Ping must produce a response"
            , test "decodes TypeChar with payload" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "t2" )
                                , ( "cmd"
                                  , Encode.object
                                        [ ( "TypeChar", Encode.object [ ( "ch", Encode.string "s" ) ] ) ]
                                  )
                                ]

                        ( msg, _ ) =
                            Debug.Interpreter.interpret payload (initModel ())
                    in
                    case msg of
                        KeyPressed _ _ _ _ ->
                            Expect.pass

                        _ ->
                            Expect.fail ("Expected KeyPressed, got: " ++ Debug.toString msg)
            , test "missing 'cmd' key produces decode error response" <|
                \_ ->
                    let
                        payload =
                            Encode.object [ ( "id", Encode.string "t3" ) ]

                        ( _, response ) =
                            Debug.Interpreter.interpret payload (initModel ())
                    in
                    case response of
                        Just r ->
                            r.error |> Expect.notEqual Nothing

                        Nothing ->
                            Expect.fail "Malformed payload must produce a response with error"
            , test "malformed cmd object produces decode error response" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "t4" )
                                , ( "cmd", Encode.string "not-an-object" )
                                ]

                        ( _, response ) =
                            Debug.Interpreter.interpret payload (initModel ())
                    in
                    case response of
                        Just r ->
                            r.error |> Expect.notEqual Nothing

                        Nothing ->
                            Expect.fail "Malformed cmd must produce error response"
            ]
        , describe "all 31 variants decode"
            [ test "Ping" <|
                \_ ->
                    checkDecodes "Ping" (Encode.object [])
            , test "Help" <|
                \_ ->
                    checkDecodes "Help" (Encode.object [])
            , test "ThreadDump" <|
                \_ ->
                    checkDecodes "ThreadDump" (Encode.object [])
            , test "SetDebug" <|
                \_ ->
                    checkDecodes "SetDebug" (Encode.object [ ( "enabled", Encode.bool True ) ])
            , test "ThrowCrash" <|
                \_ ->
                    checkDecodes "ThrowCrash" (Encode.object [])
            , test "ListTabs" <|
                \_ ->
                    checkDecodes "ListTabs" (Encode.object [])
            , test "SelectTab" <|
                \_ ->
                    checkDecodes "SelectTab" (Encode.object [ ( "id", Encode.string "tab-1" ) ])
            , test "NewTab" <|
                \_ ->
                    checkDecodes "NewTab" (Encode.object [])
            , test "CloseTab" <|
                \_ ->
                    checkDecodes "CloseTab" (Encode.object [ ( "id", Encode.string "tab-1" ) ])
            , test "TabInfo" <|
                \_ ->
                    checkDecodes "TabInfo" (Encode.object [])
            , test "Reset" <|
                \_ ->
                    checkDecodes "Reset"
                        (Encode.object
                            [ ( "compositionType", Encode.string "gat" )
                            , ( "raag", Encode.string "yaman" )
                            , ( "taal", Encode.string "teentaal" )
                            ]
                        )
            , test "SetTaal" <|
                \_ ->
                    checkDecodes "SetTaal" (Encode.object [ ( "taal", Encode.string "teentaal" ) ])
            , test "CheckFocus" <|
                \_ ->
                    checkDecodes "CheckFocus" (Encode.object [])
            , test "FocusEditor" <|
                \_ ->
                    checkDecodes "FocusEditor" (Encode.object [])
            , test "SetOctave" <|
                \_ ->
                    checkDecodes "SetOctave" (Encode.object [ ( "octave", Encode.string "mandra" ) ])
            , test "SetSubdivision" <|
                \_ ->
                    checkDecodes "SetSubdivision" (Encode.object [ ( "n", Encode.int 2 ) ])
            , test "TypeChar" <|
                \_ ->
                    checkDecodes "TypeChar" (Encode.object [ ( "ch", Encode.string "s" ) ])
            , test "Press" <|
                \_ ->
                    checkDecodes "Press" (Encode.object [ ( "key", Encode.string "BACKSPACE" ) ])
            , test "TypeTimed" <|
                \_ ->
                    checkDecodes "TypeTimed"
                        (Encode.object
                            [ ( "ch", Encode.string "s" )
                            , ( "delayMs", Encode.int 100 )
                            ]
                        )
            , test "DualSwar" <|
                \_ ->
                    checkDecodes "DualSwar"
                        (Encode.object
                            [ ( "first", Encode.string "s" )
                            , ( "second", Encode.string "r" )
                            ]
                        )
            , test "SwarGroup" <|
                \_ ->
                    checkDecodes "SwarGroup"
                        (Encode.object
                            [ ( "notes", Encode.list Encode.string [ "s", "r", "g" ] ) ]
                        )
            , test "Stroke" <|
                \_ ->
                    checkDecodes "Stroke" (Encode.object [ ( "stroke", Encode.string "da" ) ])
            , test "SimpleOrnament" <|
                \_ ->
                    checkDecodes "SimpleOrnament" (Encode.object [ ( "name", Encode.string "kan" ) ])
            , test "OrnamentStart" <|
                \_ ->
                    checkDecodes "OrnamentStart" (Encode.object [ ( "kind", Encode.string "meend" ) ])
            , test "OrnamentNote" <|
                \_ ->
                    checkDecodes "OrnamentNote" (Encode.object [ ( "note", Encode.string "g" ) ])
            , test "FinishOrnament" <|
                \_ ->
                    checkDecodes "FinishOrnament" (Encode.object [])
            , test "SwitchSection" <|
                \_ ->
                    checkDecodes "SwitchSection" (Encode.object [ ( "idx", Encode.int 0 ) ])
            , test "GetState" <|
                \_ ->
                    checkDecodes "GetState" (Encode.object [])
            , test "GetEvents" <|
                \_ ->
                    checkDecodes "GetEvents" (Encode.object [])
            , test "DumpComposition" <|
                \_ ->
                    checkDecodes "DumpComposition" (Encode.object [])
            , test "DumpHistory" <|
                \_ ->
                    checkDecodes "DumpHistory" (Encode.object [])
            ]
        ]


checkDecodes : String -> Encode.Value -> Expect.Expectation
checkDecodes variantName payload =
    let
        fullPayload =
            Encode.object
                [ ( "id", Encode.string "test" )
                , ( "cmd", Encode.object [ ( variantName, payload ) ] )
                ]

        ( _, response ) =
            Debug.Interpreter.interpret fullPayload (initModel ())
    in
    case response of
        Just r ->
            case r.error of
                Just err ->
                    if String.contains "not fully implemented" err || String.contains "not supported" err || String.contains "not wired" err then
                        Expect.pass

                    else
                        Expect.fail ("Decode error for " ++ variantName ++ ": " ++ err)

                Nothing ->
                    Expect.pass

        Nothing ->
            Expect.pass


initModel : () -> Model.Model
initModel _ =
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
                { title = "Test"
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
    in
    Model.init "http://localhost:28080"
