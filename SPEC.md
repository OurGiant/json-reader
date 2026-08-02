# JSON Viewer — Design Notes

## Hand-rolled JSON parsing vs. Jackson

`core/JsonProcessor` hand-rolls formatting, compacting, stringifying,
bracket-matching validation, and tokenizing rather than pulling in Jackson
(the library every other data-format-handling sibling project uses, e.g.
kiro-control-panel).

Kept intentionally:

- The app's actual job is character-level: pretty-printing with exact
  indentation control, syntax-highlighting token spans by character
  position, and lenient linting that still reports *something* on
  malformed input (unmatched brackets, trailing commas) rather than
  just failing to parse. A DOM/tree-model library like Jackson would
  still need a custom char-level pass on top for the highlighting and
  lenient-lint behavior, so adopting it wouldn't remove the hand-rolled
  code — it would only add a dependency alongside it.
- No data binding, schema validation, or structural transformation
  happens anywhere in the app — the one thing Jackson would otherwise
  be doing instead of the hand-rolled code.
- Zero-dependency JSON handling keeps the fat jar small; FlatLaf/SLF4J
  are UI/logging concerns, not core-logic concerns, so this doesn't
  contradict pulling those in (#4).

Revisit this if the app ever needs real JSON Schema validation, JSON Path
queries, or structural diffing — at that point a library earns its keep
on its own merits, not as a replacement for the tokenizer.

Untrusted-input hardening (size caps, depth limits) for this hand-rolled
parser is tracked separately — see README's Hardening section.
