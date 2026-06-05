port module Ports exposing
    ( downloadFile
    , downloadBinaryFile
    , exportPdf
    , selectFile
    , fileSelected
    , fileLoaded
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


port exportPdf : Value -> Cmd msg


port selectFile : String -> Cmd msg


-- INCOMING PORTS (JS -> Elm)


port fileSelected : (String -> msg) -> Sub msg


port fileLoaded : (String -> msg) -> Sub msg
