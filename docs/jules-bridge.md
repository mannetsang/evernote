# Jules bridge

How Jules (through the Jules webhook) and Claude talk to each other in this
repository: by GitHub issues and pull-request comments. It is the same bridge
as `GenC-Beauty/genc-pos`.

```
Jules ──opens issue / comments──▶ GitHub ──jules-bridge.yml──▶ Jules webhook ──▶ Jules
                                    │
                                    └──issue-relay.yml──▶ comment on the relay PR ──▶ Claude (subscribed)
Claude ──replies on the issue or PR (<!-- claude -->)──▶ GitHub ──jules-bridge.yml──▶ Jules
```

## The two workflows

| Workflow | Runs on | Does |
|---|---|---|
| `.github/workflows/jules-bridge.yml` | issue opened, edited, reopened, closed or labelled; any issue or PR comment created or edited | POSTs the event as JSON to `JULES_WEBHOOK_URL` with `Authorization: Bearer JULES_WEBHOOK_KEY`. Skipped when the body contains `<!-- jules -->`. |
| `.github/workflows/issue-relay.yml` | issue opened or reopened; any issue or PR comment created; pull request opened, reopened or marked ready for review; review submitted; inline review comment | Posts a one-line "doorbell" comment on the open PR labelled `claude-relay`, which a Claude session is subscribed to, so one subscription covers the whole repository. Skipped for anything on the relay PR itself, for its own comments (`github-actions[bot]`), and for anything carrying `<!-- claude -->`. Other bots are not skipped, so Jules still wakes Claude if it posts through one. Fails loudly when no relay PR is open. |

## Markers (loop guards)

| Writer | Puts this in every issue or comment it writes | Effect |
|---|---|---|
| Jules | `<!-- jules -->` | Not sent back to Jules. Claude is still woken. |
| Claude | `<!-- claude -->` | Does not wake Claude again. Still sent to Jules. |
| The relay workflow | `<!-- jules -->` on its doorbell comments | Doorbells are not forwarded to Jules. |

## Setup

1. **Actions secrets** on mannetsang/evernote (Settings → Secrets and variables → Actions): `JULES_WEBHOOK_URL` and `JULES_WEBHOOK_KEY`. Man copies the same values used on GenC POS (one shared URL and key). Until the URL is set, the Jules job prints a notice and passes.
2. **Jules's webhook receiver** must accept events from this repo and be able to post back to mannetsang/evernote.
3. **Relay pull request**: one open PR labelled `claude-relay` (branch `claude-relay`, never merged), with a Claude session subscribed to its activity. If it is ever closed, the relay workflow fails until it is reopened or another open PR gets the label.
