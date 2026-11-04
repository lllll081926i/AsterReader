---
name: Aster System
colors:
  surface: '#f3fbf5'
  surface-dim: '#d4dcd6'
  surface-bright: '#f3fbf5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eef6ef'
  surface-container: '#e8f0ea'
  surface-container-high: '#e2eae4'
  surface-container-highest: '#dce4de'
  on-surface: '#161d1a'
  on-surface-variant: '#414844'
  inverse-surface: '#2a322e'
  inverse-on-surface: '#ebf3ec'
  outline: '#717974'
  outline-variant: '#c1c8c3'
  surface-tint: '#436557'
  primary: '#2b4d40'
  on-primary: '#ffffff'
  primary-container: '#436557'
  on-primary-container: '#bbe1cf'
  inverse-primary: '#aacfbd'
  secondary: '#506359'
  on-secondary: '#ffffff'
  secondary-container: '#d2e8db'
  on-secondary-container: '#56695e'
  tertiary: '#244b58'
  on-tertiary: '#ffffff'
  tertiary-container: '#3d6370'
  on-tertiary-container: '#b6deed'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c5ebd9'
  primary-fixed-dim: '#aacfbd'
  on-primary-fixed: '#002116'
  on-primary-fixed-variant: '#2c4d40'
  secondary-fixed: '#d2e8db'
  secondary-fixed-dim: '#b7cbbf'
  on-secondary-fixed: '#0d1f17'
  on-secondary-fixed-variant: '#384b41'
  tertiary-fixed: '#c1e9f8'
  tertiary-fixed-dim: '#a5cddc'
  on-tertiary-fixed: '#001f28'
  on-tertiary-fixed-variant: '#254c58'
  background: '#f3fbf5'
  on-background: '#161d1a'
  surface-variant: '#dce4de'
  paper-light: '#FBF9F5'
  paper-warm: '#F3EAD8'
  paper-dark-gray: '#1F2021'
  paper-oled-black: '#000000'
  ink-light: '#191C1B'
  ink-warm: '#2D261C'
  ink-dark: '#E1E3DF'
  vault-secure-tint: '#284D3D'
  annotation-yellow: '#F7E69C'
  annotation-green: '#C4E8C2'
  annotation-blue: '#C0DCF5'
  annotation-pink: '#F6C8D6'
typography:
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  headline-sm:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '400'
    lineHeight: 32px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '400'
    lineHeight: 28px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-expanded: 1rem
  margin: 1rem
  margin-medium: 1.5rem
  margin-expanded: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

### Personality & Values
The design system embodies a calm, focused, and utilitarian ethos: quiet, translucent, lightweight, and book-first. The product acts as an unobtrusive medium between the reader and their local collection of books and comics. Every visual cue is deliberately subdued to place reading content at the apex of visual hierarchy.

The tone avoids playful decorations, harsh gradients, or visual noise. Interaction patterns are disciplined, deterministic, and swift, reinforcing security and privacy with immediate feedback.

### Visual Style
The design merges modern **Corporate / Modern** principles (strict Material 3 Expressive compliance, dynamic Monet tonal balance, and edge-to-edge system integration) with purposeful **Minimalism**. Elevation relies exclusively on tonal shifts and subtle luminance tiers rather than heavy artificial drop shadows. Surfaces feel lightweight, slightly rounded, and structured into continuous grouped containers.

## Colors

### Global Semantic Palette
The application chrome adheres strictly to Material 3 dynamic color generation via system Monet algorithms. The hex tokens provided serve as the canonical baseline palette when OEM wallpaper extraction is unavailable, generating balanced low-saturation tonal palettes.
- **Primary (`#436557`)**: Natural, muted botanical sage. Drives primary calls-to-action, high-prominence selection, and active progress milestones.
- **Secondary (`#506359`)**: Muted slate green. Applied to tonal chips, non-active segment selections, and supporting interactive surfaces.
- **Tertiary (`#3e6471`)**: Muted slate teal. Applied to nuanced auxiliary callouts, library metrics, and metadata badges.
- **Neutral (`#717974`)**: Structural cool neutral. Produces the complete `surface`, `surfaceContainer`, and outline spectrums.

### Decoupled Reader Color Space
To protect reading focus, the reading engine viewport is decoupled from system Monet wallpaper shifting. It operates exclusively on four dedicated canvas profiles:
- **Light Paper (`#FBF9F5`)**: High-contrast, optical reading surface paired with `ink-light` typography.
- **Warm Paper (`#F3EAD8`)**: Sepia-tuned, eye-strain reducing paper canvas paired with `ink-warm` typography.
- **Dark Gray (`#1F2021`)**: Default canvas for comics and low-glare reading; pairs with `ink-dark` text.
- **OLED Black (`#000000`)**: Zero-luminance black for absolute battery preservation on OLED panels.
- **Annotation Palette**: Tint highlights are kept low-saturation and non-glare, ensuring readability across both paper and dark environments.

## Typography

### Hierarchy & Scaling Philosophy
App UI typography leverages the adaptive system font (`Roboto Flex` on Android 12+) to support dynamic accessibility font scaling up to 200% and 300% without clipped layouts or broken baselines.

- **Headline & Title Tiers**: `headline-sm` handles primary destination views (Library, Shelves, Vault). `title-lg` labels secondary modules and hero blocks. `title-md` formats publication covers in grids and lists (clamped to a 2-line maximum ellipsis).
- **Body & Label Tiers**: `body-lg` defines settings switch labels and prominent reading summaries. `body-md` sets secondary helper text, while `body-sm` carries file metadata, chapter numbering, and sync timestamps. `label-lg` is strictly single-line, reserving space for buttons, filter chips, and interactive pill triggers.
- **Engine Isolation**: E-book reader content rendering uses its own internal typographic configuration (font files, margins, kerning, line heights) completely detached from the UI text scale tokens.

## Layout & Spacing

### Layout System
The system is constructed around an 8-point spatial system, with a 4dp (`space-xs`) half-step reserved for micro-alignments, tag internal spacing, and dense comic frame gutters.

- **Compact Mobile (<600dp)**: 
  - Margin: `margin` (16dp).
  - Gutter: `gutter` (12dp).
  - Navigation: 2-destination bottom bar with central pill floating launcher.
  - Cover Grid: 2 or 3 responsive columns with a standard 2:3 aspect ratio.
- **Medium / Foldable (600dp – 839dp)**:
  - Margin: `margin-medium` (24dp).
  - Navigation: Narrow Navigation Rail or flexible split bottom bar.
  - Cover Grid: 3 to 4 columns.
- **Expanded Desktop / Tablet (≥840dp)**:
  - Margin: `margin-expanded` (32dp).
  - Gutter: `gutter-expanded` (16dp).
  - Max Content Width: Fixed at 1080dp for central lists; side reading panels clamp to a maximum width of 480dp.
  - Structure: Adaptive dual-pane supporting pane layout.

### Edge Gestures & Touch Boundaries
To prevent collision with Android system-level back swipes, reader controls enforce an explicit `edgeGestureBuffer` of 24dp along outer display borders. All touch surfaces maintain a strict minimum dimension of 48dp (`touchTargetMin`).

## Elevation & Depth

### Depth Model
Depth is communicated through flat tonal layering and surface-container tiers. Drop shadows, layered bevels, and artificial skeuomorphism are absent. Surfaces express depth through luminosity contrast and container tinting.

### Layer Stacking Order
1. **Window Base (`surface`)**: Global canvas background.
2. **Subtle Containment (`surfaceContainerLow`)**: Grouped card containers, shelf backgrounds, and passive reading lists.
3. **Primary Surfaces (`surfaceContainer`)**: Standard input boundaries, settings groups, and the 56dp search capsule.
4. **Elevated & Floating Bars (`surfaceContainerHigh`)**: Floating reader capsule islands, navigation sheets, and transient toolbars.
5. **Interactive Nodes (`primaryContainer` / `secondaryContainer`)**: Active pills, quick-shelf tiles, and FAB actions.
6. **Modal Backing (`scrim`)**: Dimmed background backdrop behind sheets and dialogs.
7. **Secure Shield Overlay**: An absolute, opaque neutral boundary applied before UI render when inspecting the encrypted vault, preventing thumbnail and memory caching leaks.

## Shapes

### Shape Language
The design uses a rounded geometry (`roundedness: 2`, base radius 8dp / 0.5rem) combined with fully rounded pill shapes for action elements.

- **Publication Art (`CoverArtwork`)**: Fixed 12dp radius to soften book boundaries while preserving cover proportions.
- **Settings Groups (`AsterSettingsGroup`)**: 24dp outer boundary radius (`rounded-xl` / 1.5rem), presenting continuous pill-like grouping blocks. Inner rows use hairline separators rather than individual radiuses.
- **Action Islands & Capsules**: Fully rounded 999dp pill radius applied to floating reading engine controls, 2x2 shelf quick tiles, filter tags, and the 56dp search bar.

## Components

### Search Capsule (`largeSearchHeight`)
- **Resting Height**: 56dp.
- **Geometry**: Pill shape (999dp radius), filled with `surfaceContainer`.
- **Styling**: Single-line input, anchored by a 24dp search glyph on the leading edge and filter/profile chips on the trailing edge. No drop shadows; uses tonal contrast against the root `surface`.

### 2×2 Quick Shelf Entrance
- **Structure**: Symmetric 2-column by 2-row layout with an 8dp (`space-sm`) intra-item gap.
- **Geometry**: Pill-cornered cards (minimum height 96dp) using `secondaryContainer` fill.
- **Content**: Bold single-line `title-md` caption, item count badge, and a low-opacity outlined icon.

### Reader Shell (Floating Capsule Island)
- **Geometry**: 999dp pill shape floating 24dp above the bottom window edge.
- **Surface**: `surfaceContainerHigh` with subtle tonal border separation (`outlineVariant`).
- **Behavior**: Auto-hides on reader canvas tap or after a 3000ms inactivity timeout. Translates vertically by 16dp with an alpha fade (200ms duration). Never interrupts gesture reading pans.

### Settings Container (`AsterSettingsGroup`)
- **Container**: Continuous card rounded at 24dp using `surfaceContainer` fill.
- **Dividers**: Inner setting items separated by full-width 0.5dp `outlineVariant` hairlines with 16dp horizontal insets. Individual rows maintain a minimum 56dp interactive height.

### Vault Encryption State
- **Presentation**: Pure opaque neutral mask. Never uses semi-transparent blurs over unauthenticated covers.
- **Feedback**: A minimal outlined lock icon (`24dp`) centered with a single-line `bodyMedium` label ("受保护内容"). Destructive or error colors (`errorContainer`) are never used as default vault indicator colors.

### Buttons, Chips & Interactive Controls
- **Buttons**: Primary buttons use `primaryContainer` with 999dp pill radius and `label-lg` typography. Touch boundaries scale to ≥48dp.
- **Filter Chips**: Height of 32dp, pill-shaped, tinted with `surfaceContainerLow` in idle state and transitioning to `secondaryContainer` on selection with micro-scale haptic confirmation.
- **Lists & Cards**: Card items maintain a 2:3 aspect ratio for books and comics. Skeleton loaders share identical 12dp corner radii to prevent layout shift during local file indexing.