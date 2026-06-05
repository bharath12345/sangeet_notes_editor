module Input.OrnamentMode exposing
    ( OrnamentAction(..)
    , transition
    )

import Model.Types exposing (MeendDirection, NoteRef)
import State.Model exposing (OrnamentMode(..))


{-| Result of processing a key press in an ornament mode.
-}
type OrnamentAction
    = ApplySimple String
    | ApplySingleNote String NoteRef
    | ApplyMeend NoteRef NoteRef MeendDirection
    | ApplyKrintan (List NoteRef)
    | ApplyMurki (List NoteRef)
    | ApplyZamzama (List NoteRef)
    | StillCollecting OrnamentMode
    | Cancelled


{-| Transition the ornament state machine given the current mode and a note input.
Returns the appropriate action.
-}
transition : OrnamentMode -> Maybe NoteRef -> Bool -> OrnamentAction
transition mode maybeNote isEnter =
    case mode of
        NoOrnament ->
            Cancelled

        SingleNoteMode ornamentType ->
            case maybeNote of
                Just noteRef ->
                    ApplySingleNote ornamentType noteRef

                Nothing ->
                    Cancelled

        MeendStartMode direction ->
            case maybeNote of
                Just noteRef ->
                    StillCollecting (MeendEndMode noteRef direction)

                Nothing ->
                    Cancelled

        MeendEndMode startNote direction ->
            case maybeNote of
                Just endNote ->
                    ApplyMeend startNote endNote direction

                Nothing ->
                    Cancelled

        KrintanStartMode ->
            case maybeNote of
                Just noteRef ->
                    StillCollecting (KrintanEndMode noteRef)

                Nothing ->
                    Cancelled

        KrintanEndMode startNote ->
            case maybeNote of
                Just endNote ->
                    ApplyKrintan [ startNote, endNote ]

                Nothing ->
                    if isEnter then
                        ApplyKrintan [ startNote ]

                    else
                        Cancelled

        MurkiCollectMode collected ->
            if isEnter then
                if List.isEmpty collected then
                    Cancelled

                else
                    ApplyMurki (List.reverse collected)

            else
                case maybeNote of
                    Just noteRef ->
                        StillCollecting (MurkiCollectMode (noteRef :: collected))

                    Nothing ->
                        Cancelled

        ZamzamaCollectMode collected ->
            if isEnter then
                if List.isEmpty collected then
                    Cancelled

                else
                    ApplyZamzama (List.reverse collected)

            else
                case maybeNote of
                    Just noteRef ->
                        StillCollecting (ZamzamaCollectMode (noteRef :: collected))

                    Nothing ->
                        Cancelled
