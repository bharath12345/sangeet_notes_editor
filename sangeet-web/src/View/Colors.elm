module View.Colors exposing
    ( NotationColors
    , defaultColors
    )

{-| Notation color palette matching the desktop app.
These are used as fallback when the server colors have not yet loaded.
-}


type alias NotationColors =
    { taalMarker : String
    , taalMarkerSam : String
    , swar : String
    , octaveDot : String
    , ornament : String
    , stroke : String
    , sahitya : String
    , rest : String
    , sustain : String
    , komalMark : String
    , tivraMark : String
    }


{-| Default colors matching sangeet-core NotationColors.
-}
defaultColors : NotationColors
defaultColors =
    { taalMarker = "#B71C1C"
    , taalMarkerSam = "#D32F2F"
    , swar = "#1A237E"
    , octaveDot = "#E65100"
    , ornament = "#4A148C"
    , stroke = "#00695C"
    , sahitya = "#2E7D32"
    , rest = "#616161"
    , sustain = "#9E9E9E"
    , komalMark = "#1A237E"
    , tivraMark = "#1A237E"
    }
