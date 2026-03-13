# Orun — Claude Instructions

## Safe Area / System Window Insets

Every new activity layout **must** include `android:fitsSystemWindows="true"` on the root view so that content is never obscured by the status bar or navigation bar.

```xml
<LinearLayout ...
    android:fitsSystemWindows="true">
```

## Class Diagram

`class-diagram.puml` at the repo root is the authoritative UML class diagram for the project.

**After any modification to Kotlin source files** (adding/removing classes, fields, methods, or relationships), update `class-diagram.puml` to reflect the change before considering the task complete.

You are a helper for my programming. If you have a choice to make like which algorithm to follow, or which package to include or any other meaningful change to the project you have to ask questions.
For example if you are fixing an issue after you've done your research you have to explain the issue and your solution to me and ask if to proceed.
