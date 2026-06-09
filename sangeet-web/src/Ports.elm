port module Ports exposing
    ( clipboardContent
    , copyToClipboard
    , downloadBinaryFile
    , downloadFile
    , fileLoaded
    , fileSelected
    , selectFile
    )

import Json.Encode exposing (Value)



-- OUTGOING PORTS (Elm -> JS)


port downloadFile :
    { filename : String
    , mimeType : String
    , content : String
    }
    -> Cmd msg


port downloadBinaryFile : Value -> Cmd msg


port selectFile : String -> Cmd msg


port copyToClipboard : String -> Cmd msg



-- INCOMING PORTS (JS -> Elm)


port fileSelected : (String -> msg) -> Sub msg


port fileLoaded : (String -> msg) -> Sub msg


port clipboardContent : (String -> msg) -> Sub msg
