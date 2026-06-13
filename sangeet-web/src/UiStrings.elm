module UiStrings exposing (..)

-- GENERATED FILE — DO NOT EDIT MANUALLY.
-- Source:    sangeet-core/src/main/resources/ui-strings.json
-- Regenerate: cd scripts && npm run gen   (or: make gen-strings)
--
-- To add or change a string: edit ui-strings.json, then run `make gen-strings`,
-- then use `UiStrings.<key>` on both desktop and web. See
-- docs/developer/ui-strings-catalog.md for the full guide.


dialogAboutBetaNote : String
dialogAboutBetaNote =
    "Beta release — actively iterating toward v1.0. Expect rough edges; please file bugs via the 🐞 Report bug button in the toolbar."


dialogAboutBetaNoteDesktop : String
dialogAboutBetaNoteDesktop =
    "Beta release — actively iterating toward v1.0. Expect rough edges; please file bugs via the 🐞 button in the toolbar."


dialogAboutClose : String
dialogAboutClose =
    "Close"


dialogAboutCopyright : String
dialogAboutCopyright =
    "© 2026 Bharadwaj. "


dialogAboutDescriptionDesktopLine1 : String
dialogAboutDescriptionDesktopLine1 =
    "A notation editor for Hindustani classical music in the Bhatkhande style."


dialogAboutDescriptionDesktopLine2 : String
dialogAboutDescriptionDesktopLine2 =
    "Designed primarily for sitar compositions — Gat, Bandish, and Palta."


dialogAboutDescriptionParagraph1 : String
dialogAboutDescriptionParagraph1 =
    "A notation editor for Hindustani classical music in the Bhatkhande style. Built for sitar compositions: gat, bandish, palta — with mizrab strokes, meend, kan swar, gamak, and the full Bhatkhande notation set."


dialogAboutDescriptionParagraph2 : String
dialogAboutDescriptionParagraph2 =
    "Supports Devanagari, Kannada, Telugu, and English scripts."


dialogAboutLicense : String
dialogAboutLicense =
    "Free and open source under the MIT License."


dialogAboutLicenseDesktop : String
dialogAboutLicenseDesktop =
    "Free and open source. Copyright (c) 2026 Bharadwaj."


dialogAboutLinksDownloadDesktop : String
dialogAboutLinksDownloadDesktop =
    "Download desktop app"


dialogAboutLinksGithub : String
dialogAboutLinksGithub =
    "GitHub repository"


dialogAboutLinksHeader : String
dialogAboutLinksHeader =
    "Links"


dialogAboutLinksLicense : String
dialogAboutLinksLicense =
    "MIT License"


dialogAboutLinksSelfHosting : String
dialogAboutLinksSelfHosting =
    "Self-hosting guide"


dialogAboutLinksUserGuide : String
dialogAboutLinksUserGuide =
    "User guide"


dialogAboutLinksUserGuideDesktop : String
dialogAboutLinksUserGuideDesktop =
    "User guide & documentation"


dialogAboutPrivacyDesktop : String
dialogAboutPrivacyDesktop =
    "Anonymous usage stats (which features get touched, how long sessions are — never the content you type) are sent to PostHog so I can prioritise what to build next. Set the SANGEET_ANALYTICS_DISABLED=1 environment variable to turn this off."


dialogAboutPrivacyHeader : String
dialogAboutPrivacyHeader =
    "Privacy"


dialogAboutPrivacyText : String
dialogAboutPrivacyText =
    "While you use the app, anonymous usage events (clicks, keystrokes — never the text content of fields) are sent to PostHog so I can see which features people actually reach for. If you click \"🐞 Report bug\", the last few minutes of your activity in this page are recorded as a video-like replay and sent along with your message so I can reproduce what you saw. Password fields are never captured. Nothing leaves your browser unless you click Send. Reports auto-delete from storage after 90 days. The desktop app sends a smaller, separate set of anonymous events to a different PostHog project for the same reason; users can opt out by setting SANGEET_ANALYTICS_DISABLED=1."


dialogAboutSampleToggle : String
dialogAboutSampleToggle =
    "Show sample composition on startup"


dialogAboutSupportLink : String
dialogAboutSupportLink =
    "Support the project"


dialogAboutSupportSuffix : String
dialogAboutSupportSuffix =
    " — UPI / PayPal options."


dialogAboutSupportText : String
dialogAboutSupportText =
    "💖 "


dialogAboutTech : String
dialogAboutTech =
    "Desktop: Scala 3 + ScalaFX. Web: Elm + Tapir."


dialogAboutTechDesktop : String
dialogAboutTechDesktop =
    "Built with Scala 3 + ScalaFX (desktop) and Elm + Tapir (web)"


dialogAboutTitle : String
dialogAboutTitle =
    "Sangeet Notes Editor"


dialogBugReportButtonCancel : String
dialogBugReportButtonCancel =
    "Cancel"


dialogBugReportButtonSend : String
dialogBugReportButtonSend =
    "Send"


dialogBugReportButtonSending : String
dialogBugReportButtonSending =
    "Sending..."


dialogBugReportButtonSentSuccess : String
dialogBugReportButtonSentSuccess =
    "Sent ✓"


dialogBugReportDescriptionLabel : String
dialogBugReportDescriptionLabel =
    "What went wrong? What were you trying to do?"


dialogBugReportDescriptionPlaceholder : String
dialogBugReportDescriptionPlaceholder =
    "The more detail the better — keys pressed, expected vs actual, etc."


dialogBugReportDisclosureDesktop : String
dialogBugReportDisclosureDesktop =
    "We'll include a short replay of recent keystrokes + a screenshot of this window + the active composition (the .swar JSON of the tab you have open) so the bug can be reproduced. Password fields aren't typed in this app at all. Nothing leaves your machine until you click Send."


dialogBugReportDisclosureWeb : String
dialogBugReportDisclosureWeb =
    "We'll include a short replay of your recent actions in the app (the last few minutes only) so the bug can be reproduced. Password fields are never captured. Nothing leaves your browser until you click Send below."


dialogBugReportEmailLabel : String
dialogBugReportEmailLabel =
    "Email (optional, only if you want a reply)"


dialogBugReportEmailPlaceholder : String
dialogBugReportEmailPlaceholder =
    "you@example.com"


dialogBugReportStatusSending : String
dialogBugReportStatusSending =
    "Sending report..."


dialogBugReportTitle : String
dialogBugReportTitle =
    "Report a bug"


dialogCommandPaletteNoResults : String
dialogCommandPaletteNoResults =
    "No matching actions."


dialogCommandPaletteSearchPlaceholder : String
dialogCommandPaletteSearchPlaceholder =
    "Search actions… (Esc to close)"


dialogCommandPaletteSearchPlaceholderWeb : String
dialogCommandPaletteSearchPlaceholderWeb =
    "Search actions… (Esc to close, ↑↓ to navigate, Enter to run)"


dialogCommandPaletteTitle : String
dialogCommandPaletteTitle =
    "Command Palette"


dialogKeyboardCheatSheetActionAddSection : String
dialogKeyboardCheatSheetActionAddSection =
    "Add section"


dialogKeyboardCheatSheetActionCloseTab : String
dialogKeyboardCheatSheetActionCloseTab =
    "Close tab"


dialogKeyboardCheatSheetActionCompositionProperties : String
dialogKeyboardCheatSheetActionCompositionProperties =
    "Composition properties"


dialogKeyboardCheatSheetActionCopy : String
dialogKeyboardCheatSheetActionCopy =
    "Copy"


dialogKeyboardCheatSheetActionCut : String
dialogKeyboardCheatSheetActionCut =
    "Cut"


dialogKeyboardCheatSheetActionCycleScript : String
dialogKeyboardCheatSheetActionCycleScript =
    "Cycle notation script"


dialogKeyboardCheatSheetActionExportHtml : String
dialogKeyboardCheatSheetActionExportHtml =
    "Export HTML"


dialogKeyboardCheatSheetActionNewComposition : String
dialogKeyboardCheatSheetActionNewComposition =
    "New composition"


dialogKeyboardCheatSheetActionNextTab : String
dialogKeyboardCheatSheetActionNextTab =
    "Next tab"


dialogKeyboardCheatSheetActionOpenFile : String
dialogKeyboardCheatSheetActionOpenFile =
    "Open file"


dialogKeyboardCheatSheetActionOpenFolder : String
dialogKeyboardCheatSheetActionOpenFolder =
    "Open folder"


dialogKeyboardCheatSheetActionOpenUserGuide : String
dialogKeyboardCheatSheetActionOpenUserGuide =
    "Open user guide"


dialogKeyboardCheatSheetActionPaste : String
dialogKeyboardCheatSheetActionPaste =
    "Paste"


dialogKeyboardCheatSheetActionPreviousTab : String
dialogKeyboardCheatSheetActionPreviousTab =
    "Previous tab"


dialogKeyboardCheatSheetActionRedo : String
dialogKeyboardCheatSheetActionRedo =
    "Redo"


dialogKeyboardCheatSheetActionRemoveSection : String
dialogKeyboardCheatSheetActionRemoveSection =
    "Remove current section"


dialogKeyboardCheatSheetActionRenameSection : String
dialogKeyboardCheatSheetActionRenameSection =
    "Rename current section"


dialogKeyboardCheatSheetActionReportBug : String
dialogKeyboardCheatSheetActionReportBug =
    "Report a bug"


dialogKeyboardCheatSheetActionSave : String
dialogKeyboardCheatSheetActionSave =
    "Save"


dialogKeyboardCheatSheetActionSaveAs : String
dialogKeyboardCheatSheetActionSaveAs =
    "Save as"


dialogKeyboardCheatSheetActionShowCheatSheet : String
dialogKeyboardCheatSheetActionShowCheatSheet =
    "Show this cheat sheet"


dialogKeyboardCheatSheetActionToggleFileBrowser : String
dialogKeyboardCheatSheetActionToggleFileBrowser =
    "Toggle file browser"


dialogKeyboardCheatSheetActionToggleTheme : String
dialogKeyboardCheatSheetActionToggleTheme =
    "Toggle theme"


dialogKeyboardCheatSheetActionUndo : String
dialogKeyboardCheatSheetActionUndo =
    "Undo"


dialogKeyboardCheatSheetButtonClose : String
dialogKeyboardCheatSheetButtonClose =
    "Close"


dialogKeyboardCheatSheetHintDesktopFull : String
dialogKeyboardCheatSheetHintDesktopFull =
    "-shortcuts wired (browsers reserve many of them on web). Full reference:"


dialogKeyboardCheatSheetHintKeyboardRef : String
dialogKeyboardCheatSheetHintKeyboardRef =
    "Keyboard Reference"


dialogKeyboardCheatSheetHintWeb : String
dialogKeyboardCheatSheetHintWeb =
    "Tip: most toolbar actions are accessible via the buttons above. The desktop app has the full set of"


dialogKeyboardCheatSheetLabelCancelOrnament : String
dialogKeyboardCheatSheetLabelCancelOrnament =
    "Cancel ornament mode"


dialogKeyboardCheatSheetLabelChikari : String
dialogKeyboardCheatSheetLabelChikari =
    "Chikari (open strings)"


dialogKeyboardCheatSheetLabelCutCopyPaste : String
dialogKeyboardCheatSheetLabelCutCopyPaste =
    "Cut / Copy / Paste"


dialogKeyboardCheatSheetLabelDaRaStrokes : String
dialogKeyboardCheatSheetLabelDaRaStrokes =
    "Da (inward) / Ra (outward)"


dialogKeyboardCheatSheetLabelDeleteEvent : String
dialogKeyboardCheatSheetLabelDeleteEvent =
    "Delete event"


dialogKeyboardCheatSheetLabelDoubleTapDual : String
dialogKeyboardCheatSheetLabelDoubleTapDual =
    "Double-tap dual swar"


dialogKeyboardCheatSheetLabelExtendSelection : String
dialogKeyboardCheatSheetLabelExtendSelection =
    "Extend selection"


dialogKeyboardCheatSheetLabelFastTyping : String
dialogKeyboardCheatSheetLabelFastTyping =
    "Type 2–4 notes within 500 ms to auto-group"


dialogKeyboardCheatSheetLabelFinishOrnament : String
dialogKeyboardCheatSheetLabelFinishOrnament =
    "Finish multi-note ornament"


dialogKeyboardCheatSheetLabelGamakAndolan : String
dialogKeyboardCheatSheetLabelGamakAndolan =
    "Gamak / Andolan / Gitkari"


dialogKeyboardCheatSheetLabelKanSwar : String
dialogKeyboardCheatSheetLabelKanSwar =
    "Kan swar"


dialogKeyboardCheatSheetLabelKomalRe : String
dialogKeyboardCheatSheetLabelKomalRe =
    "Komal Re / Ga / Dha / Ni"


dialogKeyboardCheatSheetLabelMadhyaDefault : String
dialogKeyboardCheatSheetLabelMadhyaDefault =
    "Madhya (default)"


dialogKeyboardCheatSheetLabelMandraLower : String
dialogKeyboardCheatSheetLabelMandraLower =
    "Mandra (lower)"


dialogKeyboardCheatSheetLabelMeendDown : String
dialogKeyboardCheatSheetLabelMeendDown =
    "Meend ↓"


dialogKeyboardCheatSheetLabelMeendUp : String
dialogKeyboardCheatSheetLabelMeendUp =
    "Meend ↑"


dialogKeyboardCheatSheetLabelMoveCursor : String
dialogKeyboardCheatSheetLabelMoveCursor =
    "Move cursor one beat"


dialogKeyboardCheatSheetLabelNextSubbeat : String
dialogKeyboardCheatSheetLabelNextSubbeat =
    "Next sub-beat"


dialogKeyboardCheatSheetLabelRest : String
dialogKeyboardCheatSheetLabelRest =
    "Rest"


dialogKeyboardCheatSheetLabelSetNotesPerBeat : String
dialogKeyboardCheatSheetLabelSetNotesPerBeat =
    "Set notes per beat"


dialogKeyboardCheatSheetLabelShowCheatSheet : String
dialogKeyboardCheatSheetLabelShowCheatSheet =
    "Show this cheat sheet"


dialogKeyboardCheatSheetLabelShuddhaSwaras : String
dialogKeyboardCheatSheetLabelShuddhaSwaras =
    "Shuddha swaras"


dialogKeyboardCheatSheetLabelSparsh : String
dialogKeyboardCheatSheetLabelSparsh =
    "Sparsh"


dialogKeyboardCheatSheetLabelSustain : String
dialogKeyboardCheatSheetLabelSustain =
    "Sustain"


dialogKeyboardCheatSheetLabelTaarUpper : String
dialogKeyboardCheatSheetLabelTaarUpper =
    "Taar (upper)"


dialogKeyboardCheatSheetLabelTivraMa : String
dialogKeyboardCheatSheetLabelTivraMa =
    "Tivra Ma"


dialogKeyboardCheatSheetLabelUndoRedo : String
dialogKeyboardCheatSheetLabelUndoRedo =
    "Undo / Redo"


dialogKeyboardCheatSheetSectionEditDesktop : String
dialogKeyboardCheatSheetSectionEditDesktop =
    "Edit"


dialogKeyboardCheatSheetSectionFileDesktop : String
dialogKeyboardCheatSheetSectionFileDesktop =
    "File"


dialogKeyboardCheatSheetSectionHelpDesktop : String
dialogKeyboardCheatSheetSectionHelpDesktop =
    "Help"


dialogKeyboardCheatSheetSectionHelpWeb : String
dialogKeyboardCheatSheetSectionHelpWeb =
    "Help"


dialogKeyboardCheatSheetSectionNavigation : String
dialogKeyboardCheatSheetSectionNavigation =
    "Navigation"


dialogKeyboardCheatSheetSectionOctave : String
dialogKeyboardCheatSheetSectionOctave =
    "Octave (saptak)"


dialogKeyboardCheatSheetSectionOrnaments : String
dialogKeyboardCheatSheetSectionOrnaments =
    "Ornaments"


dialogKeyboardCheatSheetSectionSectionsDesktop : String
dialogKeyboardCheatSheetSectionSectionsDesktop =
    "Sections"


dialogKeyboardCheatSheetSectionSelectionClipboard : String
dialogKeyboardCheatSheetSectionSelectionClipboard =
    "Selection & clipboard"


dialogKeyboardCheatSheetSectionStrokes : String
dialogKeyboardCheatSheetSectionStrokes =
    "Strokes"


dialogKeyboardCheatSheetSectionSubdivisions : String
dialogKeyboardCheatSheetSectionSubdivisions =
    "Subdivisions"


dialogKeyboardCheatSheetSectionSwar : String
dialogKeyboardCheatSheetSectionSwar =
    "Swar (notes)"


dialogKeyboardCheatSheetSectionTabsDesktop : String
dialogKeyboardCheatSheetSectionTabsDesktop =
    "Tabs"


dialogKeyboardCheatSheetSectionViewDesktop : String
dialogKeyboardCheatSheetSectionViewDesktop =
    "View"


dialogKeyboardCheatSheetSubtitleDesktop : String
dialogKeyboardCheatSheetSubtitleDesktop =
    "Full reference: Help → User Guide → Keyboard Reference"


dialogKeyboardCheatSheetTitle : String
dialogKeyboardCheatSheetTitle =
    "Keyboard Shortcuts"


dialogNewCompositionButtonCancel : String
dialogNewCompositionButtonCancel =
    "Cancel"


dialogNewCompositionButtonCreate : String
dialogNewCompositionButtonCreate =
    "Create"


dialogNewCompositionFieldAntaraStartingBeatLabelDesktop : String
dialogNewCompositionFieldAntaraStartingBeatLabelDesktop =
    "Antara Starting Beat:"


dialogNewCompositionFieldArohanLabel : String
dialogNewCompositionFieldArohanLabel =
    "Arohan:"


dialogNewCompositionFieldArohanPlaceholder : String
dialogNewCompositionFieldArohanPlaceholder =
    "auto-detected or enter manually"


dialogNewCompositionFieldAvrohanLabel : String
dialogNewCompositionFieldAvrohanLabel =
    "Avrohan:"


dialogNewCompositionFieldAvrohanPlaceholder : String
dialogNewCompositionFieldAvrohanPlaceholder =
    "auto-detected or enter manually"


dialogNewCompositionFieldFilePathBrowseButton : String
dialogNewCompositionFieldFilePathBrowseButton =
    "Browse..."


dialogNewCompositionFieldFilePathBrowserTitle : String
dialogNewCompositionFieldFilePathBrowserTitle =
    "Save Composition As"


dialogNewCompositionFieldFilePathLabel : String
dialogNewCompositionFieldFilePathLabel =
    "Save to:"


dialogNewCompositionFieldFilePathPlaceholder : String
dialogNewCompositionFieldFilePathPlaceholder =
    "Select location to save .swar file"


dialogNewCompositionFieldGatStartingBeatLabelDesktop : String
dialogNewCompositionFieldGatStartingBeatLabelDesktop =
    "Gat Starting Beat:"


dialogNewCompositionFieldLayaAtidrut : String
dialogNewCompositionFieldLayaAtidrut =
    "Ati-drut"


dialogNewCompositionFieldLayaAtidrutDesktop : String
dialogNewCompositionFieldLayaAtidrutDesktop =
    "Ati-Drut"


dialogNewCompositionFieldLayaAtivilambit : String
dialogNewCompositionFieldLayaAtivilambit =
    "Ati-vilambit"


dialogNewCompositionFieldLayaAtivilambitDesktop : String
dialogNewCompositionFieldLayaAtivilambitDesktop =
    "Ati-Vilambit"


dialogNewCompositionFieldLayaDrut : String
dialogNewCompositionFieldLayaDrut =
    "Drut"


dialogNewCompositionFieldLayaLabel : String
dialogNewCompositionFieldLayaLabel =
    "Laya"


dialogNewCompositionFieldLayaLabelDesktop : String
dialogNewCompositionFieldLayaLabelDesktop =
    "Laya:"


dialogNewCompositionFieldLayaMadhya : String
dialogNewCompositionFieldLayaMadhya =
    "Madhya"


dialogNewCompositionFieldLayaNone : String
dialogNewCompositionFieldLayaNone =
    "None (Palta)"


dialogNewCompositionFieldLayaNoneDesktop : String
dialogNewCompositionFieldLayaNoneDesktop =
    "(none)"


dialogNewCompositionFieldLayaVilambit : String
dialogNewCompositionFieldLayaVilambit =
    "Vilambit"


dialogNewCompositionFieldRaagLabel : String
dialogNewCompositionFieldRaagLabel =
    "Raag"


dialogNewCompositionFieldRaagLabelDesktop : String
dialogNewCompositionFieldRaagLabelDesktop =
    "Raag:"


dialogNewCompositionFieldRaagPlaceholder : String
dialogNewCompositionFieldRaagPlaceholder =
    "Type to search or enter custom raag"


dialogNewCompositionFieldSamvadiLabel : String
dialogNewCompositionFieldSamvadiLabel =
    "Samvadi:"


dialogNewCompositionFieldSamvadiPlaceholder : String
dialogNewCompositionFieldSamvadiPlaceholder =
    "auto-detected"


dialogNewCompositionFieldScriptLabel : String
dialogNewCompositionFieldScriptLabel =
    "Script:"


dialogNewCompositionFieldShowSahityaCheckboxDesktop : String
dialogNewCompositionFieldShowSahityaCheckboxDesktop =
    "Show lyrics row below swar"


dialogNewCompositionFieldShowSahityaLabel : String
dialogNewCompositionFieldShowSahityaLabel =
    "Show Sahitya Line (Lyrics)"


dialogNewCompositionFieldShowSahityaLabelDesktop : String
dialogNewCompositionFieldShowSahityaLabelDesktop =
    "Sahitya line:"


dialogNewCompositionFieldShowStrokesCheckboxDesktop : String
dialogNewCompositionFieldShowStrokesCheckboxDesktop =
    "Show Da/Ra stroke indicators below swar"


dialogNewCompositionFieldShowStrokesLabel : String
dialogNewCompositionFieldShowStrokesLabel =
    "Show Stroke Line (Da/Ra)"


dialogNewCompositionFieldShowStrokesLabelDesktop : String
dialogNewCompositionFieldShowStrokesLabelDesktop =
    "Stroke line:"


dialogNewCompositionFieldSthayiStartingBeatLabelDesktop : String
dialogNewCompositionFieldSthayiStartingBeatLabelDesktop =
    "Sthayi Starting Beat:"


dialogNewCompositionFieldTaalLabel : String
dialogNewCompositionFieldTaalLabel =
    "Taal"


dialogNewCompositionFieldTaalLabelDesktop : String
dialogNewCompositionFieldTaalLabelDesktop =
    "Taal:"


dialogNewCompositionFieldTaanCountLabel : String
dialogNewCompositionFieldTaanCountLabel =
    "Taan Count"


dialogNewCompositionFieldTaanCountLabelDesktop : String
dialogNewCompositionFieldTaanCountLabelDesktop =
    "Taans:"


dialogNewCompositionFieldTaanStartingBeatLabelDesktop : String
dialogNewCompositionFieldTaanStartingBeatLabelDesktop =
    "Taan Starting Beat:"


dialogNewCompositionFieldThaatLabel : String
dialogNewCompositionFieldThaatLabel =
    "Thaat:"


dialogNewCompositionFieldThaatPlaceholder : String
dialogNewCompositionFieldThaatPlaceholder =
    "auto-detected or enter manually"


dialogNewCompositionFieldTitleLabel : String
dialogNewCompositionFieldTitleLabel =
    "Title"


dialogNewCompositionFieldTitleLabelDesktop : String
dialogNewCompositionFieldTitleLabelDesktop =
    "Title:"


dialogNewCompositionFieldTitlePlaceholder : String
dialogNewCompositionFieldTitlePlaceholder =
    "Enter composition title"


dialogNewCompositionFieldTitlePlaceholderDesktop : String
dialogNewCompositionFieldTitlePlaceholderDesktop =
    "e.g. Yaman Vilambit Gat"


dialogNewCompositionFieldTypeBandish : String
dialogNewCompositionFieldTypeBandish =
    "Bandish (Vocal)"


dialogNewCompositionFieldTypeBandishDesktop : String
dialogNewCompositionFieldTypeBandishDesktop =
    "Bandish"


dialogNewCompositionFieldTypeGat : String
dialogNewCompositionFieldTypeGat =
    "Gat (Instrumental)"


dialogNewCompositionFieldTypeGatDesktop : String
dialogNewCompositionFieldTypeGatDesktop =
    "Gat"


dialogNewCompositionFieldTypeLabel : String
dialogNewCompositionFieldTypeLabel =
    "Type"


dialogNewCompositionFieldTypeLabelDesktop : String
dialogNewCompositionFieldTypeLabelDesktop =
    "Type:"


dialogNewCompositionFieldTypePalta : String
dialogNewCompositionFieldTypePalta =
    "Palta (Practice)"


dialogNewCompositionFieldTypePaltaDesktop : String
dialogNewCompositionFieldTypePaltaDesktop =
    "Palta"


dialogNewCompositionFieldTypeSargam : String
dialogNewCompositionFieldTypeSargam =
    "Sargam (Practice)"


dialogNewCompositionFieldTypeSargamDesktop : String
dialogNewCompositionFieldTypeSargamDesktop =
    "Sargam"


dialogNewCompositionFieldVadiLabel : String
dialogNewCompositionFieldVadiLabel =
    "Vadi:"


dialogNewCompositionFieldVadiPlaceholder : String
dialogNewCompositionFieldVadiPlaceholder =
    "auto-detected"


dialogNewCompositionHeader : String
dialogNewCompositionHeader =
    "Create a new composition"


dialogNewCompositionRaagNotFound : String
dialogNewCompositionRaagNotFound =
    "(raag not in database -- enter details manually)"


dialogNewCompositionTitle : String
dialogNewCompositionTitle =
    "New Composition"


dialogNewCompositionValidationFilePathRequired : String
dialogNewCompositionValidationFilePathRequired =
    "File path is required"


dialogNewCompositionValidationLayaRequired : String
dialogNewCompositionValidationLayaRequired =
    "Laya is required for Gat"


dialogNewCompositionValidationRaagRequired : String
dialogNewCompositionValidationRaagRequired =
    "Raag is required"


dialogNewCompositionValidationTitleRequired : String
dialogNewCompositionValidationTitleRequired =
    "Title is required"


dialogPropertiesButtonCancel : String
dialogPropertiesButtonCancel =
    "Cancel"


dialogPropertiesButtonSave : String
dialogPropertiesButtonSave =
    "Save"


dialogPropertiesFieldAntaraStartingBeatLabelDesktop : String
dialogPropertiesFieldAntaraStartingBeatLabelDesktop =
    "Antara Starting Beat:"


dialogPropertiesFieldGatStartingBeatLabelDesktop : String
dialogPropertiesFieldGatStartingBeatLabelDesktop =
    "Gat Starting Beat:"


dialogPropertiesFieldRaagLabel : String
dialogPropertiesFieldRaagLabel =
    "Raag:"


dialogPropertiesFieldSthayiStartingBeatLabelDesktop : String
dialogPropertiesFieldSthayiStartingBeatLabelDesktop =
    "Sthayi Starting Beat:"


dialogPropertiesFieldTaalLabel : String
dialogPropertiesFieldTaalLabel =
    "Taal"


dialogPropertiesFieldTaalLabelDesktop : String
dialogPropertiesFieldTaalLabelDesktop =
    "Taal:"


dialogPropertiesFieldTaanStartingBeatLabelDesktop : String
dialogPropertiesFieldTaanStartingBeatLabelDesktop =
    "Taan Starting Beat:"


dialogPropertiesFieldTitleLabel : String
dialogPropertiesFieldTitleLabel =
    "Title"


dialogPropertiesFieldTitleLabelDesktop : String
dialogPropertiesFieldTitleLabelDesktop =
    "Title:"


dialogPropertiesFieldTitlePlaceholder : String
dialogPropertiesFieldTitlePlaceholder =
    "Composition title"


dialogPropertiesFieldTypeLabel : String
dialogPropertiesFieldTypeLabel =
    "Type:"


dialogPropertiesHeader : String
dialogPropertiesHeader =
    "Edit composition details"


dialogPropertiesTitle : String
dialogPropertiesTitle =
    "Composition Properties"


dialogSupportClose : String
dialogSupportClose =
    "Close"


dialogSupportInternationalHeader : String
dialogSupportInternationalHeader =
    "For international users"


dialogSupportInternationalPaypalLink : String
dialogSupportInternationalPaypalLink =
    "Support via PayPal"


dialogSupportIntro : String
dialogSupportIntro =
    "Sangeet Notes Editor is free and always will be — all features, no restrictions. If it has helped you preserve or share music, you can support continued development:"


dialogSupportThankYou : String
dialogSupportThankYou =
    "🙏 Thank you for your support."


dialogSupportTitle : String
dialogSupportTitle =
    "Support the Project"


dialogSupportUpiHandle : String
dialogSupportUpiHandle =
    "bharath12345-1@oksbi"


dialogSupportUpiHandleLabel : String
dialogSupportUpiHandleLabel =
    "UPI handle: "


dialogSupportUpiHeader : String
dialogSupportUpiHeader =
    "For users in India — UPI"


dialogSupportUpiQrAlt : String
dialogSupportUpiQrAlt =
    "UPI QR code"


dialogSupportUpiQrPlaceholder : String
dialogSupportUpiQrPlaceholder =
    "(QR code image will appear here)"


dialogSupportWindowTitle : String
dialogSupportWindowTitle =
    "Support — Sangeet Notes Editor"


toolbarBetaBadge : String
toolbarBetaBadge =
    "BETA"


toolbarBetaTooltip : String
toolbarBetaTooltip =
    "Beta software — actively iterating toward v1.0. Use 🐞 Report bug for issues."


toolbarEditRedo : String
toolbarEditRedo =
    "Redo"


toolbarEditRedoTooltip : String
toolbarEditRedoTooltip =
    "Redo (Ctrl+Y)"


toolbarEditRedoTooltipDesktop : String
toolbarEditRedoTooltipDesktop =
    "Redo (Ctrl+Shift+Z)"


toolbarEditUndo : String
toolbarEditUndo =
    "Undo"


toolbarEditUndoTooltip : String
toolbarEditUndoTooltip =
    "Undo (Ctrl+Z)"


toolbarEditUndoTooltipDesktop : String
toolbarEditUndoTooltipDesktop =
    "Undo last edit (Ctrl+Z)"


toolbarFileCopyTooltip : String
toolbarFileCopyTooltip =
    "Copy (Ctrl+C)"


toolbarFileCopyTooltipDesktop : String
toolbarFileCopyTooltipDesktop =
    "Copy selected events (Ctrl+C)"


toolbarFileCutTooltip : String
toolbarFileCutTooltip =
    "Cut (Ctrl+X)"


toolbarFileCutTooltipDesktop : String
toolbarFileCutTooltipDesktop =
    "Cut selected events (Ctrl+X)"


toolbarFileExportHtml : String
toolbarFileExportHtml =
    "HTML"


toolbarFileExportHtmlTooltip : String
toolbarFileExportHtmlTooltip =
    "Export HTML"


toolbarFileExportHtmlTooltipDesktop : String
toolbarFileExportHtmlTooltipDesktop =
    "Export composition as HTML"


toolbarFileNew : String
toolbarFileNew =
    "New"


toolbarFileNewTooltip : String
toolbarFileNewTooltip =
    "New Composition (Ctrl+N)"


toolbarFileNewTooltipDesktop : String
toolbarFileNewTooltipDesktop =
    "Create a new composition"


toolbarFileOpen : String
toolbarFileOpen =
    "Open"


toolbarFileOpenTooltip : String
toolbarFileOpenTooltip =
    "Open File"


toolbarFileOpenTooltipDesktop : String
toolbarFileOpenTooltipDesktop =
    "Open a .swar file"


toolbarFileOpenFolderTooltip : String
toolbarFileOpenFolderTooltip =
    "Open a folder in the file browser"


toolbarFilePasteTooltip : String
toolbarFilePasteTooltip =
    "Paste (Ctrl+V)"


toolbarFilePasteTooltipDesktop : String
toolbarFilePasteTooltipDesktop =
    "Paste clipboard events (Ctrl+V)"


toolbarFileSave : String
toolbarFileSave =
    "Save"


toolbarFileSaveTooltip : String
toolbarFileSaveTooltip =
    "Save File (Ctrl+S)"


toolbarFileSaveTooltipDesktop : String
toolbarFileSaveTooltipDesktop =
    "Save composition to current file"


toolbarFileSaveAsTooltip : String
toolbarFileSaveAsTooltip =
    "Save composition as a new .swar file"


toolbarHelpAbout : String
toolbarHelpAbout =
    "About"


toolbarHelpAboutTooltip : String
toolbarHelpAboutTooltip =
    "About"


toolbarHelpAboutTooltipDesktop : String
toolbarHelpAboutTooltipDesktop =
    "About Sangeet Notes Editor"


toolbarHelpKeyboardShortcuts : String
toolbarHelpKeyboardShortcuts =
    "?"


toolbarHelpKeyboardShortcutsTooltip : String
toolbarHelpKeyboardShortcutsTooltip =
    "Keyboard shortcuts (?)"


toolbarHelpKeyboardShortcutsTooltipDesktop : String
toolbarHelpKeyboardShortcutsTooltipDesktop =
    "Show keyboard shortcuts (?)"


toolbarHelpProperties : String
toolbarHelpProperties =
    "Properties"


toolbarHelpPropertiesTooltip : String
toolbarHelpPropertiesTooltip =
    "Composition Properties"


toolbarHelpPropertiesTooltipDesktop : String
toolbarHelpPropertiesTooltipDesktop =
    "Edit composition metadata"


toolbarHelpReportBug : String
toolbarHelpReportBug =
    "🐞 Report bug"


toolbarHelpReportBugTooltip : String
toolbarHelpReportBugTooltip =
    "Report a bug — includes a short replay so it can be reproduced"


toolbarHelpReportBugTooltipDesktop : String
toolbarHelpReportBugTooltipDesktop =
    "Report a bug — includes a screenshot + recent keystrokes + the open composition"


toolbarHelpSupport : String
toolbarHelpSupport =
    "💖"


toolbarHelpSupportTooltip : String
toolbarHelpSupportTooltip =
    "Support the project — donate via UPI or PayPal"


toolbarHelpSupportTooltipDesktop : String
toolbarHelpSupportTooltipDesktop =
    "Support the project"


toolbarHelpUserGuideTooltip : String
toolbarHelpUserGuideTooltip =
    "Open the user guide (F1)"


toolbarModeStroke : String
toolbarModeStroke =
    "Mode: Stroke"


toolbarModeSwar : String
toolbarModeSwar =
    "Mode: Swar"


toolbarOrnamentKrintanEnd : String
toolbarOrnamentKrintanEnd =
    "Krintan: type end note / Enter"


toolbarOrnamentKrintanStart : String
toolbarOrnamentKrintanStart =
    "Krintan: type start note"


toolbarOrnamentMeendEnd : String
toolbarOrnamentMeendEnd =
    "Meend: type end note"


toolbarOrnamentMeendStart : String
toolbarOrnamentMeendStart =
    "Meend: type start note"


toolbarScriptDevanagari : String
toolbarScriptDevanagari =
    "Devanagari"


toolbarScriptDevanagariDesktop : String
toolbarScriptDevanagariDesktop =
    "Devanagari (Hindi)"


toolbarScriptEnglish : String
toolbarScriptEnglish =
    "English"


toolbarScriptKannada : String
toolbarScriptKannada =
    "Kannada"


toolbarScriptLabel : String
toolbarScriptLabel =
    "Script:"


toolbarScriptTelugu : String
toolbarScriptTelugu =
    "Telugu"


toolbarScriptTooltip : String
toolbarScriptTooltip =
    "Change notation script"


toolbarSectionAddTooltip : String
toolbarSectionAddTooltip =
    "Add Section"


toolbarSectionAddTooltipDesktop : String
toolbarSectionAddTooltipDesktop =
    "Add a new section to the composition"


toolbarSectionMoveDownTooltip : String
toolbarSectionMoveDownTooltip =
    "Move section down"


toolbarSectionMoveUpTooltip : String
toolbarSectionMoveUpTooltip =
    "Move section up"


toolbarSectionRemoveTooltip : String
toolbarSectionRemoveTooltip =
    "Remove current section"


toolbarSectionRemoveTooltipDesktop : String
toolbarSectionRemoveTooltipDesktop =
    "Remove the current section"


toolbarSectionRenameTooltip : String
toolbarSectionRenameTooltip =
    "Rename current section"


toolbarSectionRenameTooltipDesktop : String
toolbarSectionRenameTooltipDesktop =
    "Rename the current section (F2)"


toolbarTabsCloseTooltip : String
toolbarTabsCloseTooltip =
    "Close tab"


toolbarTabsNewTooltip : String
toolbarTabsNewTooltip =
    "New Tab"


toolbarThemeToggleTooltip : String
toolbarThemeToggleTooltip =
    "Toggle light / dark theme"


toolbarViewToggleKeyboardLegend : String
toolbarViewToggleKeyboardLegend =
    "Keys"


toolbarViewToggleKeyboardLegendTooltip : String
toolbarViewToggleKeyboardLegendTooltip =
    "Keyboard Shortcuts"


toolbarViewToggleSahityaLine : String
toolbarViewToggleSahityaLine =
    "Sahitya"


toolbarViewToggleSahityaLineTooltip : String
toolbarViewToggleSahityaLineTooltip =
    "Toggle Sahitya Line"


toolbarViewToggleStrokeLine : String
toolbarViewToggleStrokeLine =
    "Strokes"


toolbarViewToggleStrokeLineTooltip : String
toolbarViewToggleStrokeLineTooltip =
    "Toggle Stroke Line"


dialogAboutLinksWebVersion : String -> String
dialogAboutLinksWebVersion url =
    "Web version: " ++ url


dialogAboutVersion : String -> String
dialogAboutVersion version =
    "Version " ++ version


dialogBugReportStatusScreenshotFailed : String -> String
dialogBugReportStatusScreenshotFailed error =
    "Screenshot failed (" ++ error ++ ") — sending without it."


dialogBugReportStatusSendFailed : String -> String
dialogBugReportStatusSendFailed error =
    "Send failed: " ++ error


dialogBugReportStatusSendThrew : String -> String
dialogBugReportStatusSendThrew message =
    "Send threw: " ++ message


dialogBugReportStatusSent : String -> String
dialogBugReportStatusSent reportId =
    "Sent. Report id: " ++ reportId


dialogNewCompositionFieldAntaraStartingBeatLabel : Int -> String
dialogNewCompositionFieldAntaraStartingBeatLabel matras =
    "Antara Starting Beat (1-" ++ String.fromInt matras ++ ")"


dialogNewCompositionFieldGatStartingBeatLabel : Int -> String
dialogNewCompositionFieldGatStartingBeatLabel matras =
    "Gat Starting Beat (1-" ++ String.fromInt matras ++ ")"


dialogNewCompositionFieldSthayiStartingBeatLabel : Int -> String
dialogNewCompositionFieldSthayiStartingBeatLabel matras =
    "Sthayi Starting Beat (1-" ++ String.fromInt matras ++ ")"


dialogNewCompositionFieldTaanStartingBeatLabel : Int -> String
dialogNewCompositionFieldTaanStartingBeatLabel matras =
    "Taan Starting Beat (1-" ++ String.fromInt matras ++ ")"


dialogNewCompositionRaagDetected : String -> String
dialogNewCompositionRaagDetected name =
    "Raag " ++ name ++ " recognized"


dialogPropertiesFieldSectionStartingBeatLabel : String -> Int -> String
dialogPropertiesFieldSectionStartingBeatLabel name matras =
    name ++ " Starting Beat (1-" ++ String.fromInt matras ++ ")"


dialogPropertiesValidationBeatsClamped : Int -> String
dialogPropertiesValidationBeatsClamped matras =
    "Starting beats clamped to new taal range (1-" ++ String.fromInt matras ++ ")"


dialogSupportInternationalPlatformLink : String -> String
dialogSupportInternationalPlatformLink platform =
    "Support via " ++ platform


dialogSupportUpiHandleLabelWithValue : String -> String
dialogSupportUpiHandleLabelWithValue handle =
    "UPI handle: " ++ handle


toolbarOrnamentMurki : Int -> String
toolbarOrnamentMurki count =
    "Murki: " ++ String.fromInt count ++ " notes (Enter to apply)"


toolbarOrnamentSingleNote : String -> String
toolbarOrnamentSingleNote name =
    "Orn: " ++ name ++ " (type note)"


toolbarOrnamentZamzama : Int -> String
toolbarOrnamentZamzama count =
    "Zamzama: " ++ String.fromInt count ++ " notes (Enter to apply)"
