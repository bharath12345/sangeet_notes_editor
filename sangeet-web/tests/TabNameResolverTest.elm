module TabNameResolverTest exposing (suite)

import Expect
import Test exposing (Test, describe, test)
import Util.TabNameResolver as Resolver


suite : Test
suite =
    describe "TabNameResolver"
        [ describe "hasCollision"
            [ test "detects exact match" <|
                \_ -> Resolver.hasCollision "abc" [ "xyz", "abc", "def" ] |> Expect.equal True
            , test "no match returns False" <|
                \_ -> Resolver.hasCollision "abc" [ "xyz", "def" ] |> Expect.equal False
            , test "empty list returns False" <|
                \_ -> Resolver.hasCollision "abc" [] |> Expect.equal False
            , test "case-sensitive" <|
                \_ -> Resolver.hasCollision "abc" [ "ABC" ] |> Expect.equal False
            ]
        , describe "nextAvailableTitle"
            [ test "starts at (2) when the base name is the only collision" <|
                \_ -> Resolver.nextAvailableTitle "abc" [ "abc" ] |> Expect.equal "abc (2)"
            , test "picks (3) when (2) is taken" <|
                \_ -> Resolver.nextAvailableTitle "abc" [ "abc", "abc (2)" ] |> Expect.equal "abc (3)"
            , test "picks (4) when (2) and (3) are taken" <|
                \_ -> Resolver.nextAvailableTitle "abc" [ "abc", "abc (2)", "abc (3)" ] |> Expect.equal "abc (4)"
            , test "picks lowest free N even with gaps" <|
                \_ ->
                    Resolver.nextAvailableTitle "abc" [ "abc", "abc (3)", "abc (5)" ]
                        |> Expect.equal "abc (2)"
            , test "does not double-up the suffix on an already-renamed title" <|
                \_ ->
                    Resolver.nextAvailableTitle "abc (2)" [ "abc", "abc (2)" ]
                        |> Expect.equal "abc (3)"
            , test "works against an empty set" <|
                \_ -> Resolver.nextAvailableTitle "abc" [] |> Expect.equal "abc (2)"
            ]
        , describe "stripParenSuffix"
            [ test "removes trailing (N)" <|
                \_ -> Resolver.stripParenSuffix "abc (2)" |> Expect.equal "abc"
            , test "removes trailing (NN)" <|
                \_ -> Resolver.stripParenSuffix "abc (10)" |> Expect.equal "abc"
            , test "leaves plain title alone" <|
                \_ -> Resolver.stripParenSuffix "abc" |> Expect.equal "abc"
            , test "leaves non-numeric parenthetical alone" <|
                \_ -> Resolver.stripParenSuffix "abc (notnum)" |> Expect.equal "abc (notnum)"
            ]
        ]
