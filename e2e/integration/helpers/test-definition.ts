// Mirrors sangeet-core/.../debug/TestDefinition.scala.
// circe encodes Scala 3 enums as { "VariantName": { ...fields... } } at the top level.
// Keep these types in sync with that encoding shape.

export type DebugCommand =
  | { Ping: Record<string, never> }
  | { Help: Record<string, never> }
  | { ThreadDump: Record<string, never> }
  | { SetDebug: { enabled: boolean } }
  | { ThrowCrash: Record<string, never> }
  | { ListTabs: Record<string, never> }
  | { SelectTab: { id: string } }
  | { NewTab: Record<string, never> }
  | { CloseTab: { id: string } }
  | { TabInfo: Record<string, never> }
  | { Reset: { compositionType: string; raag?: string; taal: string } }
  | { SetTaal: { taal: string } }
  | { CheckFocus: Record<string, never> }
  | { FocusEditor: Record<string, never> }
  | { SetOctave: { octave: string } }
  | { SetSubdivision: { n: number } }
  | { TypeChar: { ch: string } }
  | { Press: { key: string } }
  | { TypeTimed: { ch: string; delayMs: number } }
  | { DualSwar: { first: string; second: string } }
  | { SwarGroup: { notes: string[] } }
  | { Stroke: { stroke: string } }
  | { SimpleOrnament: { name: string } }
  | { OrnamentStart: { kind: string } }
  | { OrnamentNote: { note: string } }
  | { FinishOrnament: Record<string, never> }
  | { SwitchSection: { idx: number } }
  | { GetState: Record<string, never> }
  | { GetEvents: Record<string, never> }
  | { DumpComposition: Record<string, never> }
  | { DumpHistory: Record<string, never> };

export interface ExpectedState {
  eventCount?: number;
  cursorBeat?: number;
  cursorCycle?: number;
  sectionName?: string;
  taalName?: string;
  raagName?: string;
  sectionCount?: number;
}

export type TestStep =
  | { Cmd: { cmd: DebugCommand } }
  | { Checkpoint: { expect: ExpectedState } }
  | { AssertGoldenSwar: { fixture: string } }
  | { AssertGoldenHtml: { fixture: string } };

export interface TestDefinition {
  name: string;
  description?: string;
  steps: TestStep[];
}
