module ThemeTest exposing (suite)

{-| Tests for the Theme model and the ToggleTheme update handler.

The Theme port → DOM side-effect can't be observed in elm-test (no JS
side); these tests cover the pure parts:

  - parseTheme handles both case-insensitive matches and unknown values
  - themeName round-trips
  - ToggleTheme flips Light ↔ Dark in the model

The DOM/persistence half is exercised by the e2e suite (which boots
real JS and inspects body[data-theme]).

-}

import Expect
import State.Model as Model exposing (Theme(..))
import State.Msg exposing (Msg(..))
import State.Update exposing (update)
import Test exposing (Test, describe, test)
import TestHelpers exposing (defaultModel)


suite : Test
suite =
    describe "Theme"
        [ describe "parseTheme"
            [ test "parses 'light' to Light" <|
                \_ ->
                    Model.parseTheme "light" |> Expect.equal Light
            , test "parses 'dark' to Dark" <|
                \_ ->
                    Model.parseTheme "dark" |> Expect.equal Dark
            , test "is case-insensitive" <|
                \_ ->
                    Model.parseTheme "DARK" |> Expect.equal Dark
            , test "falls back to Light on unknown input" <|
                \_ ->
                    Model.parseTheme "vermillion" |> Expect.equal Light
            , test "falls back to Light on empty input" <|
                \_ ->
                    Model.parseTheme "" |> Expect.equal Light
            ]
        , describe "themeName"
            [ test "Light → 'light'" <|
                \_ ->
                    Model.themeName Light |> Expect.equal "light"
            , test "Dark → 'dark'" <|
                \_ ->
                    Model.themeName Dark |> Expect.equal "dark"
            , test "round-trips through parseTheme" <|
                \_ ->
                    Model.parseTheme (Model.themeName Dark) |> Expect.equal Dark
            ]
        , describe "ToggleTheme handler"
            [ test "Light → Dark on first toggle" <|
                \_ ->
                    let
                        ( newModel, _ ) =
                            update ToggleTheme { defaultModel | theme = Light }
                    in
                    newModel.theme |> Expect.equal Dark
            , test "Dark → Light on second toggle" <|
                \_ ->
                    let
                        ( newModel, _ ) =
                            update ToggleTheme { defaultModel | theme = Dark }
                    in
                    newModel.theme |> Expect.equal Light
            , test "two toggles return to the original theme" <|
                \_ ->
                    let
                        start =
                            { defaultModel | theme = Light }

                        ( afterOne, _ ) =
                            update ToggleTheme start

                        ( afterTwo, _ ) =
                            update ToggleTheme afterOne
                    in
                    afterTwo.theme |> Expect.equal start.theme
            ]
        , describe "Model.init"
            [ test "stores the supplied initial theme" <|
                \_ ->
                    (Model.init "http://test" Dark).theme |> Expect.equal Dark
            ]
        ]
