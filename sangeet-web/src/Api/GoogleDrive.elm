port module Api.GoogleDrive exposing
    ( createFile
    , createFolder
    , deleteItem
    , googleDriveAuthResult
    , googleDriveDirListing
    , googleDriveError
    , googleDriveFileContent
    , googleDriveWriteResult
    , initiateAuth
    , listDir
    , moveItem
    , readFile
    , renameItem
    , writeFile
    )

import Json.Decode as Decode
import Json.Encode as Encode



-- OUTGOING PORTS (Elm -> JS)


port googleDriveAuth : () -> Cmd msg


port googleDriveListDir : String -> Cmd msg


port googleDriveReadFile : String -> Cmd msg


port googleDriveWriteFile : Encode.Value -> Cmd msg


port googleDriveCreateFile : Encode.Value -> Cmd msg


port googleDriveCreateFolder : Encode.Value -> Cmd msg


port googleDriveRenameItem : Encode.Value -> Cmd msg


port googleDriveDeleteItem : String -> Cmd msg


port googleDriveMoveItem : Encode.Value -> Cmd msg



-- INCOMING PORTS (JS -> Elm)


port googleDriveAuthResult : (Decode.Value -> msg) -> Sub msg


port googleDriveDirListing : (Decode.Value -> msg) -> Sub msg


port googleDriveFileContent : (Decode.Value -> msg) -> Sub msg


port googleDriveWriteResult : (Decode.Value -> msg) -> Sub msg


port googleDriveError : (String -> msg) -> Sub msg



-- CONVENIENCE FUNCTIONS


initiateAuth : Cmd msg
initiateAuth =
    googleDriveAuth ()


listDir : String -> Cmd msg
listDir folderId =
    googleDriveListDir folderId


readFile : String -> Cmd msg
readFile fileId =
    googleDriveReadFile fileId


writeFile : { fileId : String, content : String, mimeType : String } -> Cmd msg
writeFile params =
    googleDriveWriteFile
        (Encode.object
            [ ( "fileId", Encode.string params.fileId )
            , ( "content", Encode.string params.content )
            , ( "mimeType", Encode.string params.mimeType )
            ]
        )


createFile : { name : String, parentId : String, content : String, mimeType : String } -> Cmd msg
createFile params =
    googleDriveCreateFile
        (Encode.object
            [ ( "name", Encode.string params.name )
            , ( "parentId", Encode.string params.parentId )
            , ( "content", Encode.string params.content )
            , ( "mimeType", Encode.string params.mimeType )
            ]
        )


createFolder : { name : String, parentId : String } -> Cmd msg
createFolder params =
    googleDriveCreateFolder
        (Encode.object
            [ ( "name", Encode.string params.name )
            , ( "parentId", Encode.string params.parentId )
            ]
        )


renameItem : { fileId : String, newName : String } -> Cmd msg
renameItem params =
    googleDriveRenameItem
        (Encode.object
            [ ( "fileId", Encode.string params.fileId )
            , ( "newName", Encode.string params.newName )
            ]
        )


deleteItem : String -> Cmd msg
deleteItem fileId =
    googleDriveDeleteItem fileId


moveItem : { fileId : String, newParentId : String } -> Cmd msg
moveItem params =
    googleDriveMoveItem
        (Encode.object
            [ ( "fileId", Encode.string params.fileId )
            , ( "newParentId", Encode.string params.newParentId )
            ]
        )
