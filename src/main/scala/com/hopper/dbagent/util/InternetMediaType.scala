package com.hopper.dbagent

import javax.activation.MimetypesFileTypeMap

package util {
  object ContentType {
    val mimeTypes = new MimetypesFileTypeMap()

    mimeTypes.addMimeTypes("application/msword doc DOC");
    mimeTypes.addMimeTypes("application/vnd.ms-excel xls XLS");
    mimeTypes.addMimeTypes("application/pdf pdf PDF");
    mimeTypes.addMimeTypes("application/javascript js JS");
    mimeTypes.addMimeTypes("text/xml xml XML");
    mimeTypes.addMimeTypes("text/html html htm HTML HTM");
    mimeTypes.addMimeTypes("text/plain txt text TXT TEXT");
    mimeTypes.addMimeTypes("text/css css");
    mimeTypes.addMimeTypes("image/gif gif GIF");
    mimeTypes.addMimeTypes("image/ief ief");
    mimeTypes.addMimeTypes("image/jpeg jpeg jpg jpe JPG");
    mimeTypes.addMimeTypes("image/tiff tiff tif");
    mimeTypes.addMimeTypes("image/png png PNG");
    mimeTypes.addMimeTypes("image/svg svg SVG");
    mimeTypes.addMimeTypes("image/x-xwindowdump xwd");
    mimeTypes.addMimeTypes("application/postscript ai eps ps");
    mimeTypes.addMimeTypes("application/rtf rtf");
    mimeTypes.addMimeTypes("application/x-tex tex");
    mimeTypes.addMimeTypes("application/x-texinfo texinfo texi");
    mimeTypes.addMimeTypes("application/x-troff t tr roff");
    mimeTypes.addMimeTypes("audio/basic au");
    mimeTypes.addMimeTypes("audio/midi midi mid");
    mimeTypes.addMimeTypes("audio/x-aifc aifc");
    mimeTypes.addMimeTypes("audio/x-aiff aif aiff");
    mimeTypes.addMimeTypes("audio/x-mpeg mpeg mpg");
    mimeTypes.addMimeTypes("audio/x-wav wav");
    mimeTypes.addMimeTypes("video/mpeg mpeg mpg mpe");
    mimeTypes.addMimeTypes("video/quicktime qt mov");
    mimeTypes.addMimeTypes("video/x-msvideo avi");

    def get(filePath: String): String = {
      mimeTypes.getContentType(filePath)
    }
  }
}
