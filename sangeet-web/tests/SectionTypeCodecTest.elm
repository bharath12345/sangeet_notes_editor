module SectionTypeCodecTest exposing (suite)

{-| Codec tests for the SectionType enum. Added with plan-16 PR-B B.7
when SargamSection was introduced as a dedicated variant — prior to
that, Sargam compositions wrote `"type": "palta"` to disk and the
renderer showed "Sargam (Palta)". These tests lock the new round-trip
and the backward-compat behavior for old .swar files.
-}

import Expect
import Json.Decode as Decode
import Model.Composition
    exposing
        ( SectionType(..)
        , encodeSectionType
        , sectionTypeDecoder
        )
import Test exposing (Test, describe, test)


suite : Test
suite =
    describe "SectionType codec"
        [ test "SargamSection round-trips through JSON as 'sargam'" <|
            \_ ->
                let
                    encoded =
                        encodeSectionType SargamSection

                    decoded =
                        Decode.decodeValue sectionTypeDecoder encoded
                in
                Expect.all
                    [ \_ ->
                        Expect.equal
                            (Ok "sargam")
                            (Decode.decodeValue Decode.string encoded)
                    , \_ -> Expect.equal (Ok SargamSection) decoded
                    ]
                    ()
        , test "decoder accepts the string 'sargam'" <|
            \_ ->
                let
                    decoded =
                        Decode.decodeString sectionTypeDecoder "\"sargam\""
                in
                Expect.equal (Ok SargamSection) decoded
        , test "decoder still accepts 'palta' (backward compat for old Sargam .swar files)" <|
            -- Prior to PR-B B.7, Sargam compositions stored their section
            -- with `"type": "palta"`. The decoder must keep accepting
            -- that string unchanged so existing user files load.
            \_ ->
                let
                    decoded =
                        Decode.decodeString sectionTypeDecoder "\"palta\""
                in
                Expect.equal (Ok PaltaSection) decoded
        , test "all standard variants round-trip" <|
            \_ ->
                let
                    types =
                        [ Sthayi
                        , Antara
                        , Sanchari
                        , Abhog
                        , Taan
                        , Toda
                        , Jhala
                        , PaltaSection
                        , Arohi
                        , Avarohi
                        , SargamSection
                        ]

                    roundtrip st =
                        Decode.decodeValue sectionTypeDecoder (encodeSectionType st)
                            == Ok st
                in
                Expect.equal True (List.all roundtrip types)
        , test "CustomSectionType round-trips with the custom name" <|
            \_ ->
                let
                    custom =
                        CustomSectionType "Vilambit Gat"

                    decoded =
                        Decode.decodeValue sectionTypeDecoder (encodeSectionType custom)
                in
                Expect.equal (Ok custom) decoded
        ]
