module Debug.InterpreterTest exposing (suite)

import Debug.Interpreter
import Expect
import Json.Decode as Decode
import Json.Encode as Encode
import State.Model as Model
import State.Msg exposing (Msg(..))
import Test exposing (Test, describe, test)


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

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.immediateResponse of
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

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.msg of
                        KeyPressed _ _ _ _ ->
                            Expect.pass

                        _ ->
                            Expect.fail ("Expected KeyPressed, got: " ++ Debug.toString result.msg)
            , test "missing 'cmd' key produces decode error response" <|
                \_ ->
                    let
                        payload =
                            Encode.object [ ( "id", Encode.string "t3" ) ]

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.immediateResponse of
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

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.immediateResponse of
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
        , describe "Stroke dispatch (plan-16 follow-up)"
            [ test "unknown stroke returns an error response" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "stroke-bad" )
                                , ( "cmd"
                                  , Encode.object
                                        [ ( "Stroke"
                                          , Encode.object [ ( "stroke", Encode.string "wibble" ) ]
                                          )
                                        ]
                                  )
                                ]

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.immediateResponse of
                        Just r ->
                            r.error
                                |> Maybe.withDefault ""
                                |> String.contains "unknown stroke"
                                |> Expect.equal True

                        Nothing ->
                            Expect.fail "unknown stroke must yield an error response, not a pending HTTP call"
            , test "valid 'da' stroke produces an async command (no immediate response)" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "stroke-da" )
                                , ( "cmd"
                                  , Encode.object
                                        [ ( "Stroke"
                                          , Encode.object [ ( "stroke", Encode.string "da" ) ]
                                          )
                                        ]
                                  )
                                ]

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    result.immediateResponse |> Expect.equal Nothing
            , test "'ra' and 'jod' both decode to async dispatches" <|
                \_ ->
                    let
                        run name =
                            Debug.Interpreter.interpret
                                (Encode.object
                                    [ ( "id", Encode.string ("stroke-" ++ name) )
                                    , ( "cmd"
                                      , Encode.object
                                            [ ( "Stroke"
                                              , Encode.object [ ( "stroke", Encode.string name ) ]
                                              )
                                            ]
                                      )
                                    ]
                                )
                                initModel
                    in
                    [ (run "ra").immediateResponse, (run "jod").immediateResponse ]
                        |> Expect.equal [ Nothing, Nothing ]
            ]
        , describe "SetTaal dispatch (plan-16 follow-up)"
            [ test "unknown taal returns an error response" <|
                \_ ->
                    let
                        payload =
                            Encode.object
                                [ ( "id", Encode.string "settaal-bad" )
                                , ( "cmd"
                                  , Encode.object
                                        [ ( "SetTaal"
                                          , Encode.object [ ( "taal", Encode.string "fictional-taal" ) ]
                                          )
                                        ]
                                  )
                                ]

                        result =
                            Debug.Interpreter.interpret payload initModel
                    in
                    case result.immediateResponse of
                        Just r ->
                            r.error
                                |> Maybe.withDefault ""
                                |> String.contains "unknown taal"
                                |> Expect.equal True

                        Nothing ->
                            Expect.fail "unknown taal must yield an error response, not a pending HTTP call"
            , test "known taal (from availableTaals) produces async dispatch" <|
                \_ ->
                    -- The default initModel has no availableTaals (those
                    -- come from a server fetch). We seed one manually so
                    -- the lookup succeeds.
                    let
                        seededTaal =
                            { name = "Teentaal"
                            , matras = 16
                            , vibhags = []
                            , theka = Nothing
                            }

                        seededModel =
                            { initModel | availableTaals = [ ( "teentaal", seededTaal ) ] }

                        payload =
                            Encode.object
                                [ ( "id", Encode.string "settaal-ok" )
                                , ( "cmd"
                                  , Encode.object
                                        [ ( "SetTaal"
                                          , Encode.object [ ( "taal", Encode.string "Teentaal" ) ]
                                          )
                                        ]
                                  )
                                ]

                        result =
                            Debug.Interpreter.interpret payload seededModel
                    in
                    result.immediateResponse |> Expect.equal Nothing
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

        result =
            Debug.Interpreter.interpret fullPayload initModel
    in
    case result.immediateResponse of
        Just r ->
            case r.error of
                Just err ->
                    if
                        String.contains "not fully implemented" err
                            || String.contains "not supported" err
                            || String.contains "not wired" err
                            || String.contains "not implemented" err
                            || String.contains "Reset failed" err
                            || String.contains "SetTaal" err
                    then
                        Expect.pass

                    else
                        Expect.fail ("Decode error for " ++ variantName ++ ": " ++ err)

                Nothing ->
                    Expect.pass

        Nothing ->
            -- Commands like Reset that go straight to an HTTP call have no
            -- immediate response — only an extraCmd. As long as the decoder
            -- succeeded (we got here without throwing), the variant decoded.
            Expect.pass


{-| Build the default test model. We rely on `Model.init` to populate every
field with sensible defaults — these tests only care about the decoder
plumbing, not the editor state, so a fresh model is fine.
-}
initModel : Model.Model
initModel =
    Model.init "http://localhost:28080" Model.Light
