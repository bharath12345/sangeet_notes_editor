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

toolbarOrnamentMurki : Int -> String
toolbarOrnamentMurki count =
    "Murki: " ++ String.fromInt count ++ " notes (Enter to apply)"

toolbarOrnamentSingleNote : String -> String
toolbarOrnamentSingleNote name =
    "Orn: " ++ name ++ " (type note)"

toolbarOrnamentZamzama : Int -> String
toolbarOrnamentZamzama count =
    "Zamzama: " ++ String.fromInt count ++ " notes (Enter to apply)"
