package com.varpas.sangeet.core.editor

import com.varpas.sangeet.core.model.*

enum OrnamentMode:
  case KanSwar, Sparsh, Ghaseet, KrintanStart
  case MeendStart(direction: MeendDirection)
  case MeendEnd(startRef: NoteRef, direction: MeendDirection)
  case KrintanEnd(startRef: NoteRef)
  case MurkiCollect(notes: List[NoteRef])
  case ZamzamaCollect(notes: List[NoteRef])
