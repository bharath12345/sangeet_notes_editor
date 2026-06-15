module State.Update.Section exposing
    ( handleAddSection
    , handleCancelClearSection
    , handleClearSection
    , handleConfirmClearSection
    , handleMoveSectionDown
    , handleMoveSectionUp
    , handleRemoveSection
    , handleRequestClearSection
    , handleSectionAdd
    , handleSectionClear
    , handleSectionRemove
    , handleSectionReorder
    , handleSelectSection
    )

{-| Section-management handlers: select / add / remove / clear / reorder,
plus the corresponding API response handlers that push the result into
history.
-}

import Api.Client exposing (ApiResult)
import Api.Metrics as ApiMetrics
import Api.Section as ApiSection exposing (RemoveSectionResult, ReorderSectionResult)
import Http
import Model.Composition exposing (Composition, CompositionType(..), SectionType)
import State.Model as Model exposing (Model)
import State.Msg exposing (Msg(..))
import State.Update.Helpers as Helpers
import UiStrings


handleSelectSection : Int -> Model -> ( Model, Cmd Msg )
handleSelectSection idx model =
    let
        sectionStartBeat =
            (Model.composition model).sections
                |> List.drop idx
                |> List.head
                |> Maybe.map .startingBeat
                |> Maybe.withDefault 1

        minBeat =
            sectionStartBeat - 1

        cur =
            Model.cursor model

        -- Clear selection anchor when switching sections (clipboard stays intact)
        clearedCursor =
            { cur | selectionAnchor = Nothing }

        clampedModel =
            if cur.cycle == 0 && cur.beat < minBeat then
                Helpers.updateCursorInPlace { clearedCursor | beat = minBeat, subIndex = 0 } model

            else
                Helpers.updateCursorInPlace clearedCursor model

        -- Plan 18 PR-3b: count every section switch from any UI path
        -- (toolbar chip, command palette, keyboard). No labels — single
        -- counter, easiest to chart "engagement with multi-section
        -- compositions".
        switchMetric =
            if model.currentSectionIndex /= idx then
                ApiMetrics.incrementCounter model.apiBaseUrl "sangeet_section_switch_total" []

            else
                Cmd.none
    in
    ( { clampedModel | currentSectionIndex = idx }
        |> Helpers.addLog (UiStrings.statusSwitchedToSection |> String.replace "{number}" (String.fromInt (idx + 1)))
    , Cmd.batch [ Helpers.requestLayout clampedModel, switchMetric ]
    )


handleAddSection : String -> SectionType -> Model -> ( Model, Cmd Msg )
handleAddSection name sectionType model =
    let
        comp =
            Model.composition model
    in
    if comp.metadata.compositionType /= Gat then
        ( model |> Helpers.addLog UiStrings.statusSectionsOnlyForGat
        , Cmd.none
        )

    else
        ( { model | pendingApiCall = True }
        , ApiSection.addSection model.apiBaseUrl comp name sectionType GotSectionAdd
        )


handleRemoveSection : Int -> Model -> ( Model, Cmd Msg )
handleRemoveSection idx model =
    let
        comp =
            Model.composition model
    in
    ( { model | pendingApiCall = True }
    , ApiSection.removeSection model.apiBaseUrl comp model.currentSectionIndex idx GotSectionRemove
    )


handleRequestClearSection : Int -> Model -> ( Model, Cmd Msg )
handleRequestClearSection idx model =
    ( { model | showClearSectionDialog = True, clearSectionIndex = Just idx }
    , Cmd.none
    )


handleConfirmClearSection : Model -> ( Model, Cmd Msg )
handleConfirmClearSection model =
    case model.clearSectionIndex of
        Just idx ->
            let
                comp =
                    Model.composition model
            in
            ( { model | pendingApiCall = True, showClearSectionDialog = False, clearSectionIndex = Nothing }
            , ApiSection.clearSection model.apiBaseUrl comp idx GotSectionClear
            )

        Nothing ->
            ( { model | showClearSectionDialog = False }
            , Cmd.none
            )


handleCancelClearSection : Model -> ( Model, Cmd Msg )
handleCancelClearSection model =
    ( { model | showClearSectionDialog = False, clearSectionIndex = Nothing }
    , Cmd.none
    )


handleClearSection : Int -> Model -> ( Model, Cmd Msg )
handleClearSection idx model =
    let
        comp =
            Model.composition model
    in
    ( { model | pendingApiCall = True }
    , ApiSection.clearSection model.apiBaseUrl comp idx GotSectionClear
    )


handleMoveSectionUp : Int -> Model -> ( Model, Cmd Msg )
handleMoveSectionUp idx model =
    if idx > 0 then
        let
            comp =
                Model.composition model
        in
        ( { model | pendingApiCall = True }
        , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx - 1) GotSectionReorder
        )

    else
        ( model, Cmd.none )


handleMoveSectionDown : Int -> Model -> ( Model, Cmd Msg )
handleMoveSectionDown idx model =
    let
        comp =
            Model.composition model

        maxIdx =
            List.length comp.sections - 1
    in
    if idx < maxIdx then
        ( { model | pendingApiCall = True }
        , ApiSection.reorderSections model.apiBaseUrl comp model.currentSectionIndex idx (idx + 1) GotSectionReorder
        )

    else
        ( model, Cmd.none )



-- API RESPONSE HANDLERS


handleSectionAdd : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleSectionAdd result model =
    Helpers.handleApiResult result
        (\comp ->
            let
                newModel =
                    Helpers.updateComposition comp model
                        |> Helpers.addLog UiStrings.statusSectionAdded
            in
            ( newModel, Helpers.requestLayout newModel )
        )
        model


handleSectionRemove : Result Http.Error (ApiResult RemoveSectionResult) -> Model -> ( Model, Cmd Msg )
handleSectionRemove result model =
    Helpers.handleApiResult result
        (\r ->
            let
                newModel =
                    Helpers.updateComposition r.composition model
                        |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                        |> Helpers.addLog UiStrings.statusSectionRemoved
            in
            ( newModel, Helpers.requestLayout newModel )
        )
        model


handleSectionClear : Result Http.Error (ApiResult Composition) -> Model -> ( Model, Cmd Msg )
handleSectionClear result model =
    Helpers.handleApiResult result
        (\comp ->
            let
                sections =
                    comp.sections

                sectionName =
                    sections
                        |> List.drop model.currentSectionIndex
                        |> List.head
                        |> Maybe.map .name
                        |> Maybe.withDefault "section"

                logMsg =
                    UiStrings.statusSectionCleared sectionName

                newModel =
                    Helpers.updateComposition comp model
                        |> Helpers.addLog logMsg
            in
            ( newModel, Helpers.requestLayout newModel )
        )
        model


handleSectionReorder : Result Http.Error (ApiResult ReorderSectionResult) -> Model -> ( Model, Cmd Msg )
handleSectionReorder result model =
    Helpers.handleApiResult result
        (\r ->
            let
                newModel =
                    Helpers.updateComposition r.composition model
                        |> (\m -> { m | currentSectionIndex = r.currentSectionIndex })
                        |> Helpers.addLog UiStrings.statusSectionsReordered
            in
            ( newModel, Helpers.requestLayout newModel )
        )
        model
