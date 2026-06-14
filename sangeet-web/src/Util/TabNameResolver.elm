module Util.TabNameResolver exposing
    ( hasCollision
    , nextAvailableTitle
    , stripParenSuffix
    )

{-| True if `title` already appears in `existing`. Case-sensitive — matches the displayed tab text.
-}


hasCollision : String -> List String -> Bool
hasCollision title existing =
    List.member title existing


{-| Generate a unique title by appending `(N)`. Picks the lowest N (starting at 2) such that
`"$baseTitle (N)"` is not in `existing`.

Mirrors `TabNameResolver.nextAvailableTitle` on the desktop side — same disambiguation behavior so cross-platform
test expectations stay aligned.

-}
nextAvailableTitle : String -> List String -> String
nextAvailableTitle baseTitle existing =
    let
        stripped =
            stripParenSuffix baseTitle

        loop n =
            let
                candidate =
                    stripped ++ " (" ++ String.fromInt n ++ ")"
            in
            if List.member candidate existing then
                loop (n + 1)

            else
                candidate
    in
    loop 2


{-| Strip a trailing `(N)` (one or more digits, parenthesised) from a tab title so the auto-rename suffix doesn't
compound on repeated resolutions. Pure string-walking implementation (no regex package).
-}
stripParenSuffix : String -> String
stripParenSuffix title =
    let
        trimmed =
            String.trimRight title
    in
    if String.endsWith ")" trimmed then
        case String.indexes "(" trimmed of
            [] ->
                title

            indexes ->
                let
                    openIdx =
                        Maybe.withDefault 0 (List.head (List.reverse indexes))

                    inside =
                        String.slice (openIdx + 1) (String.length trimmed - 1) trimmed
                in
                if String.toInt inside /= Nothing then
                    String.trimRight (String.left openIdx trimmed)

                else
                    title

    else
        title
