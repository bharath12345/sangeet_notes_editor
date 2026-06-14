module View.FileBrowser exposing (view)

import Html exposing (Html, button, div, li, span, text, ul)
import Html.Attributes exposing (class, classList, title)
import Html.Events exposing (onClick)
import State.Model exposing (DriveItem, DriveState(..), FolderState, Model)
import State.Msg exposing (Msg(..))
import UiStrings


view : Model -> Html Msg
view model =
    if model.fileBrowserCollapsed then
        viewCollapsed

    else
        viewExpanded model


viewCollapsed : Html Msg
viewCollapsed =
    div [ class "file-browser-panel file-browser-collapsed" ]
        [ button
            [ class "panel-toggle-btn"
            , title UiStrings.fileBrowserShowFilesTooltip
            , onClick ToggleFileBrowser
            ]
            [ text "📁" ]
        ]


viewExpanded : Model -> Html Msg
viewExpanded model =
    div [ class "file-browser-panel" ]
        [ div [ class "panel-header" ]
            [ span [ class "panel-title" ] [ text UiStrings.fileBrowserPanelTitle ]
            , button
                [ class "panel-toggle-btn"
                , title UiStrings.fileBrowserHideFilesTooltip
                , onClick ToggleFileBrowser
                ]
                [ text "←" ]
            ]
        , viewDriveSection model
        , viewFolderTree model.driveFolders
        ]


viewDriveSection : Model -> Html Msg
viewDriveSection model =
    case model.driveState of
        DriveDisconnected ->
            div [ class "drive-section" ]
                [ button
                    [ class "drive-connect-btn"
                    , onClick ConnectDrive
                    ]
                    [ text UiStrings.fileBrowserConnectDrive ]
                ]

        DriveConnecting ->
            div [ class "drive-section" ]
                [ span [ class "drive-status" ] [ text UiStrings.fileBrowserConnecting ] ]

        DriveConnected ->
            div [ class "drive-section" ]
                [ span [ class "drive-status drive-connected" ] [ text UiStrings.fileBrowserDriveConnected ] ]


viewFolderTree : List FolderState -> Html Msg
viewFolderTree folders =
    if List.isEmpty folders then
        div [ class "folder-tree-empty" ]
            [ text UiStrings.fileBrowserEmptyState ]

    else
        div [ class "folder-tree" ]
            [ ul [ class "folder-list" ]
                (List.map viewFolder folders)
            ]


viewFolder : FolderState -> Html Msg
viewFolder folder =
    li [ class "folder-item" ]
        [ div [ class "folder-header" ]
            [ button
                [ class "folder-expand-btn"
                , onClick (DriveOpenFolder folder.folderId)
                ]
                [ text
                    (if folder.expanded then
                        "▾"

                     else
                        "▸"
                    )
                ]
            , span
                [ class "folder-name"
                , onClick (DriveOpenFolder folder.folderId)
                ]
                [ text folder.name ]
            , button
                [ class "folder-bookmark-btn"
                , title
                    (if folder.isBookmarked then
                        UiStrings.fileBrowserRemoveBookmarkTooltip

                     else
                        UiStrings.fileBrowserBookmarkTooltip
                    )
                , onClick (DriveToggleBookmark folder.folderId)
                ]
                [ text
                    (if folder.isBookmarked then
                        "★"

                     else
                        "☆"
                    )
                ]
            , button
                [ class "folder-action-btn"
                , title UiStrings.fileBrowserRefreshTooltip
                , onClick (DriveRefreshFolder folder.folderId)
                ]
                [ text "↻" ]
            ]
        , if folder.expanded then
            ul [ class "file-list" ]
                (List.map (viewDriveItem folder.folderId) folder.items)

          else
            text ""
        ]


viewDriveItem : String -> DriveItem -> Html Msg
viewDriveItem parentFolderId item =
    let
        isFolder =
            item.mimeType == "application/vnd.google-apps.folder"

        isSwar =
            String.endsWith ".swar" item.name
    in
    li
        [ classList
            [ ( "drive-item", True )
            , ( "drive-item-folder", isFolder )
            , ( "drive-item-file", not isFolder )
            , ( "drive-item-swar", isSwar )
            ]
        ]
        [ span
            [ class "drive-item-icon" ]
            [ text
                (if isFolder then
                    "📁"

                 else if isSwar then
                    "🎵"

                 else
                    "📄"
                )
            ]
        , span
            [ class "drive-item-name"
            , onClick
                (if isFolder then
                    DriveOpenFolder item.id

                 else
                    DriveOpenFile item.id item.name
                )
            ]
            [ text item.name ]
        , div [ class "drive-item-actions" ]
            [ button
                [ class "drive-item-action"
                , title UiStrings.fileBrowserDeleteTooltip
                , onClick (DriveDeleteItem parentFolderId item.id)
                ]
                [ text "×" ]
            ]
        ]
